import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export type PublicUiSettings = {
  title: string;
  primaryColor: string;      // "#22C55E"
  suggestedAmounts: number[]; // [5, 20, 30, 50, 100]
};

@Injectable({ providedIn: 'root' })
export class AdminSettingsService {
  private readonly base = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  getPublicUi() {
    return this.http.get<PublicUiSettings>(`${this.base}/api/admin/settings/public-ui`);
  }

  updatePublicUi(payload: PublicUiSettings) {
    return this.http.put<PublicUiSettings>(`${this.base}/api/admin/settings/public-ui`, payload);
  }
}
