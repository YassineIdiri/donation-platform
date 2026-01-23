import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export type SettingsResponse = {
  title: string;
  suggestedAmounts: number[];
};

export type DonationResponse = {
  id: string;
  amountCents: number;
  currency: string;
  status: 'CREATED' | 'PENDING' | 'PAID' | 'FAILED';
  createdAt: string;
};

export type ReceiptResponse = {
  id: string;
  donationId: string;
  receiptNumber: number;
  status: 'REQUESTED' | 'ISSUED' | 'FAILED';
  requestedAt: string;
};

@Injectable({ providedIn: 'root' })
export class PublicApiService {
  private readonly base = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  getSettings() {
    return this.http.get<SettingsResponse>(`${this.base}/api/settings`);
  }

  createCheckoutSession(payload: {
    amount: number; // euros (int)
    email?: string;
    paymentMethod: 'CARD';
  }) {
    return this.http.post<{ donationId: string; checkoutUrl: string }>(
      `${this.base}/api/donations/checkout-session`,
      payload
    );
  }

  getDonation(id: string) {
    return this.http.get<DonationResponse>(`${this.base}/api/donations/${id}`);
  }

  requestReceipt(payload: {
    donationId: string;
    email: string;
    fullName: string;
    address: string;
  }) {
    return this.http.post<ReceiptResponse>(`${this.base}/api/receipts/request`, payload);
  }
}
