import { ErrorHandler, Injectable } from '@angular/core';
import { log } from './log';

/**
 * Catches every uncaught runtime/RxJS error in the app and logs it (with stack)
 * to `ng.error`, which ships to angular.log on the backend — so frontend
 * crashes are debuggable from the log files, not just the live console.
 */
@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  handleError(error: unknown): void {
    const err = error as { message?: string; stack?: string } | null;
    const message = err?.message ?? String(error);
    log.error('ng.error', message, { stack: err?.stack ?? null });
    // Preserve Angular's default behaviour (full object in the live console).
    console.error(error);
  }
}
