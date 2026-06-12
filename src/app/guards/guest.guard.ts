import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenService } from '../shared/token.service';
import { UserStoreService } from '../shared/user-store.service';

export const guestGuard: CanActivateFn = () => {
  const token = inject(TokenService);
  const store = inject(UserStoreService);
  const router = inject(Router);
  if (!token.isLoggedIn()) return true;

  const role = token.getRole();
  if (role === 'ROLE_ADMIN') return router.createUrlTree(['/admin/dashboard']);
  if (role === 'ROLE_AUDITOR') return router.createUrlTree(['/auditor/dashboard']);
  return router.createUrlTree(['/company/dashboard']);
};
