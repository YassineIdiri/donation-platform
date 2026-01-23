import {
  Component,
  Inject,
  OnInit,
  OnDestroy,
  ChangeDetectorRef,
  PLATFORM_ID,
} from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';

import { AdminDonationsService, DonationAdminRowResponse } from '../../../core/services/admin-donations.service';
import { PageResponse } from '../../../core/models/page-response';
import { ui, Ui } from '../../../core/utils/zoneless-ui';

@Component({
  selector: 'app-admin-donations',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-donations.component.html',
})
export class AdminDonationsComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly isBrowser: boolean;
  private readonly ui: Ui;

  page = 0;
  size = 20;
  status: 'ALL' | string = 'ALL';
  q = '';

  pageData: PageResponse<DonationAdminRowResponse>;
  loading = false;
  error: string | null = null;

  constructor(
    private readonly api: AdminDonationsService,
    private readonly cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
    this.ui = ui(this.cdr);
    this.pageData = this.emptyPage();
  }

  ngOnInit(): void {
    if (!this.isBrowser) return;
    this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private emptyPage(): PageResponse<DonationAdminRowResponse> {
    return {
      items: [],
      page: this.page,
      size: this.size,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    };
  }

  load(): void {
    this.ui.set(() => {
      this.loading = true;
      this.error = null;
    });

    this.api
      .listDonationsPage({
        page: this.page,
        size: this.size,
        status: this.status === 'ALL' ? undefined : this.status,
      })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.ui.set(() => (this.loading = false))),
        this.ui.pipeRepaint()
      )
      .subscribe({
        next: (p) => {
          this.ui.set(() => {
            this.pageData = p ?? this.emptyPage();
          });
        },
        error: () => {
          this.ui.set(() => {
            this.error = 'Unable to load donations';
            this.pageData = this.emptyPage();
          });
        },
      });
  }

  get donations(): DonationAdminRowResponse[] {
    return this.pageData.items ?? [];
  }

  get filtered(): DonationAdminRowResponse[] {
    const q = (this.q || '').trim().toLowerCase();
    return this.donations.filter((d) => {
      const matchesQ =
        !q ||
        String(d.id ?? '').toLowerCase().includes(q) ||
        String(d.emailMasked ?? '').toLowerCase().includes(q);

      const matchesStatus =
        this.status === 'ALL' || String(d.status ?? '') === this.status;

      return matchesQ && matchesStatus;
    });
  }

  nextPage(): void {
    if (this.pageData.last) return;
    this.page++;
    this.load();
  }

  prevPage(): void {
    if (this.page === 0) return;
    this.page--;
    this.load();
  }
}
