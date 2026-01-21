import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export type TaxReceiptStatus = 'REQUESTED' | 'ISSUED' | 'FAILED';

export type ReceiptAdminRow = {
  id: string;
  donationId: string;
  receiptNumber: number | null;
  status: TaxReceiptStatus;
  requestedAt: string;
  issuedAt?: string | null;
  emailMasked: string;
};

export type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};

@Injectable({ providedIn: 'root' })
export class AdminReceiptsService {
  private readonly base = environment.apiUrl;
  constructor(private readonly http: HttpClient) {}

  list(filters: { from?: string; to?: string; status?: TaxReceiptStatus | ''; page: number; size: number }) {
    let params = new HttpParams()
      .set('page', String(filters.page))
      .set('size', String(filters.size));

    if (filters.from) params = params.set('from', filters.from);
    if (filters.to) params = params.set('to', filters.to);
    if (filters.status) params = params.set('status', filters.status);

    return this.http.get<PageResponse<ReceiptAdminRow>>(`${this.base}/api/admin/receipts`, { params });
  }

  resend(receiptId: string) {
    return this.http.post(`${this.base}/api/admin/receipts/${receiptId}/resend`, {});
  }

  downloadPdf(receiptId: string) {
    return this.http.get(`${this.base}/api/admin/receipts/${receiptId}/download`, {
      responseType: 'blob',
    });
  }
}
