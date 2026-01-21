import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AdminAuthService } from '../core/services/admin-auth.service';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-layout.component.html',
})
export class AdminLayoutComponent {
  private readonly auth = inject(AdminAuthService);
  private readonly router = inject(Router);

  mobileOpen = false;
  busy = false;

  async logout() {
    if (this.busy) return;
    this.busy = true;
    this.auth.logout().subscribe({
      next: () => {
        this.busy = false;
        this.router.navigateByUrl('/admin/login');
      },
      error: () => {
        this.busy = false;
        // même si logout fail, on force la sortie
        this.auth.setAccessToken(null);
        this.router.navigateByUrl('/admin/login');
      },
    });
  }
}
