// src/app/features/admin/receipts/admin-receipts.component.ts
import {
  Component,
  Inject,
  OnInit,
  OnDestroy,
  ChangeDetectorRef,
  PLATFORM_ID,
} from '@angular/core';
import { CommonModule, DatePipe, isPlatformBrowser } from '@angular/common';
import { Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';

import {
  AdminReceiptsService,
  ReceiptAdminRow,
  TaxReceiptStatus,
} from '../../../core/services/admin-receipts.service';
import { ui, Ui } from '../../../core/utils/zoneless-ui';

@Component({
  selector: 'app-admin-receipts',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './admin-receipts.component.html',
})
export class AdminReceiptsComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly isBrowser: boolean;
  private readonly ui: Ui;

  // filters + pagination (même pattern que dons)
  page = 0;
  size = 20;
  status: TaxReceiptStatus | 'ALL' = 'ALL';
  q = '';

  // data
  pageData = this.emptyPage();
  loading = false;
  error: string | null = null;
  toast: string | null = null;
  busyId: string | null = null;

  constructor(
    private readonly api: AdminReceiptsService,
    private readonly cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
    this.ui = ui(this.cdr);
  }

  ngOnInit(): void {
    if (!this.isBrowser) return;
    this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private emptyPage() {
    return {
      items: [] as ReceiptAdminRow[],
      page: this.page,
      size: this.size,
      totalItems: 0,
      totalPages: 1,
    };
  }

  load(): void {
    this.ui.set(() => {
      this.loading = true;
      this.error = null;
    });

    this.api
      .list({
        page: this.page,
        size: this.size,
        status: this.status === 'ALL' ? '' : this.status,
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
            this.error = 'Impossible de charger les reçus.';
            this.pageData = this.emptyPage();
          });
        },
      });
  }

  get rows(): ReceiptAdminRow[] {
    return this.pageData.items ?? [];
  }

  get filtered(): ReceiptAdminRow[] {
    const q = (this.q || '').trim().toLowerCase();
    return this.rows.filter((r) => {
      const matchesQ =
        !q ||
        String(r.id ?? '').toLowerCase().includes(q) ||
        String(r.donationId ?? '').toLowerCase().includes(q) ||
        String(r.emailMasked ?? '').toLowerCase().includes(q);

      const matchesStatus =
        this.status === 'ALL' || String(r.status ?? '') === this.status;

      return matchesQ && matchesStatus;
    });
  }

  nextPage(): void {
    if (this.page + 1 >= (this.pageData.totalPages || 1)) return;
    this.page++;
    this.load();
  }

  prevPage(): void {
    if (this.page === 0) return;
    this.page--;
    this.load();
  }

  resend(id: string): void {
    if (this.busyId) return;

    this.ui.set(() => {
      this.busyId = id;
      this.error = null;
      this.toast = null;
    });

    this.api
      .resend(id)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.ui.set(() => (this.busyId = null))),
        this.ui.pipeRepaint()
      )
      .subscribe({
        next: () => {
          this.ui.set(() => (this.toast = 'Reçu renvoyé ✅'));
          this.load();
          window.setTimeout(() => this.ui.set(() => (this.toast = null)), 2500);
        },
        error: () => {
          this.ui.set(() => (this.error = 'Impossible de renvoyer le reçu.'));
        },
      });
  }

  downloadPdf(r: ReceiptAdminRow): void {
    if (!this.isBrowser) return;

    this.ui.set(() => {
      this.error = null;
      this.toast = null;
    });

    this.api
      .downloadPdf(r.id)
      .pipe(takeUntil(this.destroy$), this.ui.pipeRepaint())
      .subscribe({
        next: (blob: Blob) => {
          const label = this.receiptLabel(r);
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `recu-fiscal-${label}.pdf`;
          a.click();
          URL.revokeObjectURL(url);
        },
        error: () => {
          this.ui.set(() => (this.error = 'Téléchargement impossible.'));
        },
      });
  }

  receiptLabel(r: ReceiptAdminRow): string {
    return r.receiptNumber != null
      ? `CERFA-${String(r.receiptNumber).padStart(6, '0')}`
      : `DRAFT-${r.id}`;
  }

  badgeClass(status: TaxReceiptStatus): string {
    switch (status) {
      case 'ISSUED':
        return 'bg-emerald-100 text-emerald-800';
      case 'FAILED':
        return 'bg-red-100 text-red-800';
      case 'REQUESTED':
      default:
        return 'bg-amber-100 text-amber-800';
    }
  }
}
