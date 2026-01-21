// src/app/core/interceptors/auth.interceptor.ts
import {
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import {
  BehaviorSubject,
  catchError,
  filter,
  switchMap,
  take,
  throwError,
} from 'rxjs';

import { AdminAuthService } from '../services/admin-auth.service';

let refreshing = false;
const refreshedToken$ = new BehaviorSubject<string | null>(null);

function isApi(url: string): boolean {
  return url.includes('/api/');
}

function isAdmin(url: string): boolean {
  return url.includes('/api/admin/');
}

function isAdminAuth(url: string): boolean {
  return url.includes('/api/admin/auth/');
}

function hasBearer(req: HttpRequest<unknown>): boolean {
  const h = req.headers.get('Authorization');
  return !!h && h.startsWith('Bearer ');
}

function withBearer(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const adminAuth = inject(AdminAuthService);
  const router = inject(Router);

  // Ne touche pas aux requêtes non API
  if (!isApi(req.url)) {
    return next(req);
  }

  // Admin auth endpoints: cookies only, pas de Bearer, pas de refresh auto
  if (isAdminAuth(req.url)) {
    return next(req.clone({ withCredentials: true }));
  }

  // Sur /api/admin/**: ajouter Bearer si on a un token
  let request = req;
  if (isAdmin(req.url)) {
    const token = adminAuth.getAccessToken();
    if (token) {
      request = withBearer(req, token);
    }
  }

  return next(request).pipe(
    catchError((err: unknown) => {
      if (!(err instanceof HttpErrorResponse)) return throwError(() => err);

      // On ne refresh que si c'est un 401 sur /api/admin/**
      if (!isAdmin(req.url)) return throwError(() => err);
      if (err.status !== 401) return throwError(() => err);

      // Si on reçoit 401 sur une requête admin, on tente un refresh (cookie httpOnly)
      // et on queue les requêtes pendant le refresh.
      if (!refreshing) {
        refreshing = true;
        refreshedToken$.next(null);

        return adminAuth.refresh().pipe(
          switchMap(() => {
            refreshing = false;

            const newToken = adminAuth.getAccessToken();
            if (!newToken) {
              adminAuth.setAccessToken(null);
              router.navigateByUrl('/admin/login');
              return throwError(() => err);
            }

            refreshedToken$.next(newToken);
            return next(withBearer(req, newToken));
          }),
          catchError((refreshErr) => {
            refreshing = false;
            adminAuth.setAccessToken(null);
            refreshedToken$.next(null);
            router.navigateByUrl('/admin/login');
            return throwError(() => refreshErr);
          })
        );
      }

      // Si un refresh est déjà en cours, on attend qu'un nouveau token soit publié
      return refreshedToken$.pipe(
        filter((t): t is string => t !== null),
        take(1),
        switchMap((newToken) => next(withBearer(req, newToken)))
      );
    })
  );
};
