import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ForgotPasswordStateService {
  email = '';
  verifiedCode = '';

  setEmail(email: string): void { this.email = email; }
  setCode(code: string): void { this.verifiedCode = code; }
  clear(): void { this.email = ''; this.verifiedCode = ''; }
}