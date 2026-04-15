import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenService } from '../shared/token.service';

export const authGuard: CanActivateFn = () => {
  const token = inject(TokenService);
  const router = inject(Router);
  if (token.isLoggedIn()) return true;
  return router.createUrlTree(['/auth/login']);
};