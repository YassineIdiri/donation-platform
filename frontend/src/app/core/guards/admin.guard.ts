import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { AdminAuthService } from '../services/admin-auth.service';

export const adminGuard: CanActivateFn = () => {
  const platformId = inject(PLATFORM_ID);

  if (!isPlatformBrowser(platformId)) return true;
  const auth = inject(AdminAuthService);
  const router = inject(Router);

  // Si token présent -> ok
  if (auth.isLoggedIn()) return true;

  // Sinon -> tenter refresh cookie (silent), sinon redirect login
  return auth.ensureSession().pipe(
    map((ok) => (ok ? true : router.parseUrl('/admin/login')))
  );
};
