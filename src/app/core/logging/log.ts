/**
 * Lightweight, component-tagged console logging for the frontend.
 *
 * Mirrors the backend's component IDs (e.g. `ng.audit-workspace`, `ng.audit-bot`,
 * `ng.http`) so a single flow reads consistently across the browser console and
 * the server logs. Keep messages short and put structured detail in `data`.
 */
export type LogLevel = 'debug' | 'info' | 'warn' | 'error';

const STYLE: Record<LogLevel, string> = {
  debug: 'color:#6b7280',
  info:  'color:#059669;font-weight:600',
  warn:  'color:#d97706;font-weight:600',
  error: 'color:#dc2626;font-weight:700',
};

function emit(level: LogLevel, component: string, message: string, data?: unknown): void {
  const fn = level === 'error' ? console.error
           : level === 'warn'  ? console.warn
           : console.log;
  const prefix = `%c[${component}]`;
  if (data !== undefined) fn(prefix, STYLE[level], message, data);
  else fn(prefix, STYLE[level], message);
}

export const log = {
  debug: (component: string, message: string, data?: unknown) => emit('debug', component, message, data),
  info:  (component: string, message: string, data?: unknown) => emit('info',  component, message, data),
  warn:  (component: string, message: string, data?: unknown) => emit('warn',  component, message, data),
  error: (component: string, message: string, data?: unknown) => emit('error', component, message, data),
};

/** Short id matching the backend cid length, attached as X-Correlation-Id. */
export function newCorrelationId(): string {
  const c = globalThis.crypto;
  if (c && typeof c.randomUUID === 'function') {
    return c.randomUUID().replace(/-/g, '').slice(0, 8);
  }
  return Math.floor(Math.random() * 0xffffffff).toString(16).padStart(8, '0');
}
