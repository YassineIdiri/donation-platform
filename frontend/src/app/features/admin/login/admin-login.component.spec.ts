// src/app/features/admin/login/admin-login.component.spec.ts
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { Router } from '@angular/router';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { Subject, of, throwError } from 'rxjs';

import { AdminLoginComponent } from './admin-login.component';
import { AdminAuthService } from '../../../../app/core/services/admin-auth.service';

describe('AdminLoginComponent (Vitest)', () => {
  let fixture: ComponentFixture<AdminLoginComponent>;
  let component: AdminLoginComponent;

  let authMock: { login: ReturnType<typeof vi.fn> };
  let routerMock: { navigateByUrl: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    authMock = {
      login: vi.fn(),
    };

    routerMock = {
      navigateByUrl: vi.fn(),
    };

    // default safe
    authMock.login.mockReturnValue(of({ accessToken: 't', expiresInSeconds: 3600 }));

    await TestBed.configureTestingModule({
      imports: [AdminLoginComponent],
      providers: [
        { provide: AdminAuthService, useValue: authMock as unknown as AdminAuthService },
        { provide: Router, useValue: routerMock },
      ],
    })
      .overrideComponent(AdminLoginComponent, { set: { template: '' } })
      .compileComponents();

    fixture = TestBed.createComponent(AdminLoginComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    vi.clearAllMocks();
    TestBed.resetTestingModule();
  });

  it('should be created with an invalid form by default', () => {
    expect(component).toBeTruthy();
    expect(component.loading).toBe(false);
    expect(component.error).toBe(null);
    expect(component.form.invalid).toBe(true);
  });

  it('submit() does nothing if form is invalid', () => {
    component.form.controls.email.setValue('');
    component.form.controls.password.setValue('');
    expect(component.form.invalid).toBe(true);

    component.submit();

    expect(authMock.login).not.toHaveBeenCalled();
    expect(routerMock.navigateByUrl).not.toHaveBeenCalled();
  });

  it('submit() does nothing if loading=true', () => {
    component.loading = true;

    component.form.controls.email.setValue('admin@asso.fr');
    component.form.controls.password.setValue('123456');
    expect(component.form.valid).toBe(true);

    component.submit();

    expect(authMock.login).not.toHaveBeenCalled();
    expect(routerMock.navigateByUrl).not.toHaveBeenCalled();
  });

  it('submit() calls auth.login(email, password) and sets loading=true', () => {
    const stream$ = new Subject<any>();
    authMock.login.mockReturnValue(stream$.asObservable());

    component.form.controls.email.setValue('admin@asso.fr');
    component.form.controls.password.setValue('123456');

    component.submit();

    expect(component.error).toBe(null);
    expect(component.loading).toBe(true);
    expect(authMock.login).toHaveBeenCalledWith('admin@asso.fr', '123456');

    // cleanup
    stream$.complete();
  });

  it('success: should navigate to /admin/donations and set loading=false', () => {
    const stream$ = new Subject<any>();
    authMock.login.mockReturnValue(stream$.asObservable());

    component.form.controls.email.setValue('admin@asso.fr');
    component.form.controls.password.setValue('123456');

    component.submit();

    stream$.next({ accessToken: 't', expiresInSeconds: 3600 });
    stream$.complete();

    expect(component.loading).toBe(false);
    expect(component.error).toBe(null);
    expect(routerMock.navigateByUrl).toHaveBeenCalledWith('/admin/donations');
  });

  it('error: should set error="Invalid credentials." and set loading=false', () => {
    authMock.login.mockReturnValue(throwError(() => new Error('401')));

    component.form.controls.email.setValue('admin@asso.fr');
    component.form.controls.password.setValue('123456');

    component.submit();

    expect(component.loading).toBe(false);
    expect(component.error).toBe('Invalid credentials.');
    expect(routerMock.navigateByUrl).not.toHaveBeenCalled();
  });

  it('submit() should reset error on start', () => {
    const stream$ = new Subject<any>();
    authMock.login.mockReturnValue(stream$.asObservable());

    component.error = 'old error';

    component.form.controls.email.setValue('admin@asso.fr');
    component.form.controls.password.setValue('123456');

    component.submit();

    expect(component.error).toBe(null);
    expect(component.loading).toBe(true);

    stream$.complete();
  });
});
