// src/app/core/admin-auth/admin-auth.service.ts
import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { catchError, map, of, tap } from 'rxjs';

export interface AuthTokenResponse {
  accessToken: string;
  expiresInSeconds: number;
}

@Injectable({ providedIn: 'root' })
export class AdminAuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);

  private readonly base = environment.apiUrl;
  private readonly STORAGE_KEY = 'admin_access_token';

  private get isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }

  getAccessToken(): string | null {
    if (!this.isBrowser) return null;
    try {
      return sessionStorage.getItem(this.STORAGE_KEY);
    } catch {
      return null;
    }
  }

  setAccessToken(token: string | null): void {
    if (!this.isBrowser) return;
    try {
      if (!token) sessionStorage.removeItem(this.STORAGE_KEY);
      else sessionStorage.setItem(this.STORAGE_KEY, token);
    } catch {
      // ignore
    }
  }

  isLoggedIn(): boolean {
    return !!this.getAccessToken();
  }

  login(email: string, password: string) {
    return this.http
      .post<AuthTokenResponse>(
        `${this.base}/api/admin/auth/login`,
        { email, password },
        { withCredentials: true }
      )
      .pipe(tap(res => this.setAccessToken(res.accessToken)));
  }

  refresh() {
    return this.http
      .post<AuthTokenResponse>(
        `${this.base}/api/admin/auth/refresh`,
        {},
        { withCredentials: true }
      )
      .pipe(tap(res => this.setAccessToken(res.accessToken)));
  }

  logout() {
    return this.http
      .post<void>(
        `${this.base}/api/admin/auth/logout`,
        {},
        { withCredentials: true }
      )
      .pipe(
        catchError(() => of(void 0)),
        tap(() => this.setAccessToken(null))
      );
  }

  /**
   * Si pas de token, tente refresh via cookie (SSR-safe).
   * Renvoie true si session OK, false sinon.
   */
  ensureSession() {
    if (this.isLoggedIn()) return of(true);

    return this.refresh().pipe(
      map(() => true),
      catchError(() => {
        this.setAccessToken(null);
        return of(false);
      })
    );
  }

  logoutAndRedirect(reason: 'expired' | 'unauthorized' = 'expired') {
    this.setAccessToken(null);
    this.router.navigate(['/admin/login'], { queryParams: { reason } });
  }

  forceLogoutToLogin() {
    this.setAccessToken(null);
    this.router.navigateByUrl('/admin/login');
  }
}
