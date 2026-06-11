package com.devteam.aiauditserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Reads (or mints) an {@code X-Correlation-Id} for every request, puts it on the
 * SLF4J MDC so it appears in every log line (pattern {@code cid=%X{cid}}), echoes
 * it back on the response, and logs request start/end.
 *
 * The same id is forwarded to the FastAPI OCR service (see
 * {@code OcrOrchestratorService}) so one browser action is traceable across the
 * Angular client, this backend and the AI engine.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "cid";

    private static final Logger log = LoggerFactory.getLogger("spring.http");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cid = request.getHeader(HEADER);
        if (cid == null || cid.isBlank()) {
            cid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        }
        MDC.put(MDC_KEY, cid);
        response.setHeader(HEADER, cid);

        long t0 = System.currentTimeMillis();
        log.info("-> {} {}", request.getMethod(), request.getRequestURI());
        try {
            chain.doFilter(request, response);
        } catch (Exception ex) {
            // Log the full stack trace (with cid) for any error that escapes the
            // handler chain, then re-throw so normal error handling continues.
            log.error("request error {} {}", request.getMethod(), request.getRequestURI(), ex);
            throw ex;
        } finally {
            long ms = System.currentTimeMillis() - t0;
            log.info("<- {} {} {} ({}ms)", response.getStatus(), request.getMethod(),
                    request.getRequestURI(), ms);
            MDC.remove(MDC_KEY);
        }
    }
}
