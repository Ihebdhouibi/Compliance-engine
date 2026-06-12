import { Injectable, signal, computed } from '@angular/core';
import { User, RoleEnum } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserStoreService {

  private _user = signal<User | null>(null);

  readonly currentUser = this._user.asReadonly();

  readonly isAdmin   = computed(() => this._user()?.role === 'ROLE_ADMIN');
  readonly isAuditor = computed(() => this._user()?.role === 'ROLE_AUDITOR');
  readonly isUser    = computed(() => this._user()?.role === 'ROLE_USER');

  setUser(user: User): void {
    this._user.set(user);
  }

  clearUser(): void {
    this._user.set(null);
  }

  getRole(): RoleEnum | null {
    return this._user()?.role ?? null;
  }
}
