import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { tap } from 'rxjs';
import { log, newCorrelationId } from '../core/logging/log';

/** Collapse a full URL to `:port/path` so logs stay readable. */
function shortUrl(url: string): string {
  try {
    const u = new URL(url, location.origin);
    return `${u.port ? ':' + u.port : ''}${u.pathname}`;
  } catch {
    return url;
  }
}

/**
 * Attach an `X-Correlation-Id` to every outbound request (Spring :8080 and
 * FastAPI :8000) and log the call. The backends honour this id, so a single
 * browser action is traceable across all services by one cid.
 */
export const correlationInterceptor: HttpInterceptorFn = (req, next) => {
  const cid = newCorrelationId();
  const t0 = performance.now();
  const tagged = req.clone({ setHeaders: { 'X-Correlation-Id': cid } });

  log.debug('ng.http', `-> ${req.method} ${shortUrl(req.url)}`, { cid });

  return next(tagged).pipe(
    tap({
      next: event => {
        if (event instanceof HttpResponse) {
          const ms = Math.round(performance.now() - t0);
          log.debug('ng.http', `<- ${event.status} ${req.method} ${shortUrl(req.url)} (${ms}ms)`, { cid });
        }
      },
      error: err => {
        const ms = Math.round(performance.now() - t0);
        log.error('ng.http', `x ${err?.status ?? 'ERR'} ${req.method} ${shortUrl(req.url)} (${ms}ms)`,
          { cid, message: err?.message, error: err?.error });
      },
    })
  );
};
