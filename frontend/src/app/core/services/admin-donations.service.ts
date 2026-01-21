import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable, map } from 'rxjs';
import { PageResponse } from '../models/page-response';

export interface DonationAdminRowResponse {
  id: string;
  createdAt: string;
  amountCents: number;
  currency: string;
  status: string;
  emailMasked?: string;
  provider?: string;
  paymentMethod?: string;
}

@Injectable({ providedIn: 'root' })
export class AdminDonationsService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  listDonationsPage(opts?: {
    from?: string; // YYYY-MM-DD
    to?: string;   // YYYY-MM-DD
    status?: string;
    page?: number;
    size?: number;
  }): Observable<PageResponse<DonationAdminRowResponse>> {
    let params = new HttpParams();
    if (opts?.from) params = params.set('from', opts.from);
    if (opts?.to) params = params.set('to', opts.to);
    if (opts?.status) params = params.set('status', opts.status);
    params = params.set('page', String(opts?.page ?? 0));
    params = params.set('size', String(opts?.size ?? 20));

    return this.http.get<PageResponse<DonationAdminRowResponse>>(
      `${this.base}/api/admin/donations`,
      { params }
    );
  }

  listDonationsRows(opts?: Parameters<AdminDonationsService['listDonationsPage']>[0]) {
    return this.listDonationsPage(opts).pipe(map(p => p.items ?? []));
  }
}
