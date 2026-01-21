import { Routes } from '@angular/router';

// PUBLIC
import { HomeComponent } from './features/public/home/home.component';
import { ThanksComponent } from './features/public/thanks/thanks.component';
import { FailureComponent } from './features/public/failure/failure.component';

// ADMIN
import { AdminLayoutComponent } from './layout/admin-layout.component';
import { AdminLoginComponent } from './features/admin/login/admin-login.component';
import { AdminDonationsComponent } from './features/admin/donations/admin-donations.component';
import { AdminReceiptsComponent } from './features/admin/receipts/admin-receipts.component';
import { AdminSettingsComponent } from './features/admin/settings/admin-settings.component';
import { adminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  // ---- PUBLIC ----
  { path: '', component: HomeComponent },
  { path: 'thanks', component: ThanksComponent },
  { path: 'failure', component: FailureComponent },

  // ---- ADMIN ----
  { path: 'admin/login', component: AdminLoginComponent },

  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [adminGuard],
    children: [
      { path: 'donations', component: AdminDonationsComponent },
      { path: 'receipts', component: AdminReceiptsComponent },
       { path: 'settings', component: AdminSettingsComponent },
      { path: '', redirectTo: 'donations', pathMatch: 'full' },
    ],
  },

  { path: '**', redirectTo: '' },
];
