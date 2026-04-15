import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { TokenService } from '../shared/token.service';
import { RoleEnum } from '../models/user.model';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  const allowedRoles: RoleEnum[] = route.data['roles'];
  const userRole = tokenService.getRole();

  if (userRole && allowedRoles.includes(userRole)) return true;
  return router.createUrlTree(['/unauthorized']);
};