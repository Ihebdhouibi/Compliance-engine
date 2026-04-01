package com.devteam.aiauditserver.Tools.scurity;


import com.devteam.aiauditserver.Tools.util.TokenUtil;
import com.devteam.aiauditserver.services.auth.UserService;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class AuthFilter extends OncePerRequestFilter {

    private static final Log logger = LogFactory.getLog(AuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${auth.header}")
    private String tokenHeader;

    @Autowired
    private UserService userService;

    @Autowired
    private TokenUtil tokenUtil;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {


        String fullUrl = request.getRequestURL().toString();
        String queryString = request.getQueryString();
        if (queryString != null) {
            fullUrl += "?" + queryString;
        }
        logger.error("Incoming request URL: " + fullUrl);

        logger.trace("Entering doFilterInternal");
        logger.debug("Retrieving token from header: " + tokenHeader);

        final String header = request.getHeader(tokenHeader);
        logger.debug("Header value: " + header);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            logger.debug("Header missing or does not start with 'Bearer '. Skipping authentication filter.");
            filterChain.doFilter(request, response);
            logger.trace("Exiting doFilterInternal (no token)");
            return;
        }

        SecurityContext context = SecurityContextHolder.getContext();
        if (context.getAuthentication() != null) {
            logger.debug("Authentication already set in SecurityContext. Skipping re-authentication.");
            filterChain.doFilter(request, response);
            logger.trace("Exiting doFilterInternal (already authenticated)");
            return;
        }

        final String token = header.substring(BEARER_PREFIX.length());
        logger.debug("Extracted token: " + token);

        try {
            logger.trace("Checking if token is expired.");
            if (!tokenUtil.isTokenExpired(token)) {
                logger.trace("Token is valid (not expired). Extracting username.");
                String username = tokenUtil.getUserNameFromToken(token);
                logger.debug("Extracted username: " + username);

                logger.trace("Loading user details for username: " + username);
                UserDetails userDetails = userService.loadUserByUsername(username);
                logger.debug("User details loaded: " + userDetails);

                logger.trace("Validating token against user details.");
                if (tokenUtil.isTokenValid(token, userDetails)) {
                    logger.debug("Token is valid for the given user. Proceeding with authentication.");
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    context.setAuthentication(authentication);
                    logger.info("User '" + username + "' authenticated successfully. token : "+token);
                } else {
                    logger.error("Token validation failed for user: " + username);
                }
            } else {
                logger.warn("Token is expired.");
            }
        } catch (Exception e) {
            logger.error("Authentication failed: " + e.getMessage(), e);
        }

        logger.trace("Continuing filter chain.");
        filterChain.doFilter(request, response);
        logger.trace("Exiting doFilterInternal");
    }
}
