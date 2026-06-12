import { Injectable } from '@angular/core';
import { RoleEnum } from '../models/user.model';

const TOKEN_KEY = 'auth_token';
const ROLE_KEY  = 'auth_role';

@Injectable({ providedIn: 'root' })
export class TokenService {

  saveToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  saveRole(role: RoleEnum): void {
    localStorage.setItem(ROLE_KEY, role);
  }

  getRole(): RoleEnum | null {
    return localStorage.getItem(ROLE_KEY) as RoleEnum | null;
  }

  clear(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(ROLE_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }
}
