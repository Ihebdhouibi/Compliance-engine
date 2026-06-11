/**
 * Lightweight, component-tagged console logging for the frontend.
 *
 * Mirrors the backend's component IDs (e.g. `ng.audit-workspace`, `ng.audit-bot`,
 * `ng.http`) so a single flow reads consistently across the browser console and
 * the server logs. Keep messages short and put structured detail in `data`.
 */
import { environment } from '../../environments/environment';

export type LogLevel = 'debug' | 'info' | 'warn' | 'error';

const STYLE: Record<LogLevel, string> = {
  debug: 'color:#6b7280',
  info:  'color:#059669;font-weight:600',
  warn:  'color:#d97706;font-weight:600',
  error: 'color:#dc2626;font-weight:700',
};

// ── ship browser logs to the backend (angular.log) ───────────────────────────
interface ShipEntry { level: LogLevel; component: string; message: string; cid: string | null; data: string | null; }
const queue: ShipEntry[] = [];
let flushTimer: ReturnType<typeof setTimeout> | null = null;
const FLUSH_MS = 1500;
const MAX_QUEUE = 25;
const MAX_DATA = 10000;

function flush(): void {
  if (flushTimer) { clearTimeout(flushTimer); flushTimer = null; }
  if (!queue.length) return;
  const entries = queue.splice(0, queue.length);
  try {
    // fetch (not HttpClient) so this bypasses the correlation interceptor and
    // never logs itself — otherwise shipping a log would create a new log.
    fetch(`${environment.ragApiUrl}/logs`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ entries }),
      keepalive: true,
    }).catch(() => {});
  } catch { /* never let logging break the app */ }
}

function enqueue(level: LogLevel, component: string, message: string, data?: unknown): void {
  let cid: string | null = null;
  let dataStr: string | null = null;
  if (data !== undefined && data !== null) {
    if (typeof data === 'object' && 'cid' in (data as Record<string, unknown>)) {
      cid = String((data as Record<string, unknown>)['cid']);
    }
    try { dataStr = typeof data === 'string' ? data : JSON.stringify(data); }
    catch { dataStr = String(data); }
    if (dataStr && dataStr.length > MAX_DATA) dataStr = dataStr.slice(0, MAX_DATA);
  }
  queue.push({ level, component, message, cid, data: dataStr });
  if (queue.length >= MAX_QUEUE) flush();
  else if (!flushTimer) flushTimer = setTimeout(flush, FLUSH_MS);
}

if (typeof window !== 'undefined') {
  window.addEventListener('beforeunload', flush);
}

function emit(level: LogLevel, component: string, message: string, data?: unknown): void {
  const fn = level === 'error' ? console.error
           : level === 'warn'  ? console.warn
           : console.log;
  const prefix = `%c[${component}]`;
  if (data !== undefined) fn(prefix, STYLE[level], message, data);
  else fn(prefix, STYLE[level], message);
  enqueue(level, component, message, data);
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
