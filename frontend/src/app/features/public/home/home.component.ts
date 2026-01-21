import {
  Component,
  Inject,
  OnInit,
  OnDestroy,
  ChangeDetectorRef,
  PLATFORM_ID,
  signal,
  computed,
} from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { PublicApiService, SettingsResponse } from '../../../core/services/public-api.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './home.component.html',
})
export class HomeComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly isBrowser: boolean;

  loading = true;
  paying = false;
  error = '';
  toast = '';

  settings: SettingsResponse | null = null;

  // signal amount (ta template utilise currentAmount())
  readonly currentAmount = signal<number>(20);

  // on affichera 5 bulles max (comme ton UI)
  bubbleAmounts: number[] = [20, 50, 30, 100, 5];

  // texte CTA dynamique
  readonly ctaText = computed(() => (this.paying ? 'Redirection…' : 'Faire un don'));

  constructor(
    private readonly api: PublicApiService,
    private readonly cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngOnInit(): void {
    if (!this.isBrowser) return;
    this.loadSettings();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private repaint(): void {
    this.cdr.markForCheck();
  }

  private showToast(msg: string): void {
    this.toast = msg;
    this.repaint();
    window.setTimeout(() => {
      this.toast = '';
      this.repaint();
    }, 3000);
  }

  private loadSettings(): void {
    this.loading = true;
    this.error = '';
    this.repaint();

    this.api
      .getSettings()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
          this.repaint();
        })
      )
      .subscribe({
        next: (s) => {
          this.settings = s;

          // appliquer couleur principale en CSS variable (optionnel)
          if (this.isBrowser && s?.primaryColor) {
            document.documentElement.style.setProperty('--primary', s.primaryColor);
          }

          // adapter les bulles depuis le backend si possible
          const amounts = (s?.suggestedAmounts ?? []).filter((n) => Number.isFinite(n) && n > 0);
          if (amounts.length >= 1) {
            // on prend 5 max
            const a = amounts.slice(0, 5);
            // si <5, on complète
            while (a.length < 5) a.push([20, 50, 30, 100, 5][a.length]);
            this.bubbleAmounts = a;
            this.currentAmount.set(a[0] ?? 20);
          } else {
            this.bubbleAmounts = [20, 50, 30, 100, 5];
            this.currentAmount.set(20);
          }

          this.repaint();
        },
        error: (err) => {
          console.error(err);
          this.error = "Impossible de charger la page. Réessayez.";
          this.repaint();
        },
      });
  }

  onInputChange(ev: Event): void {
    const el = ev.target as HTMLInputElement;
    const v = Number(el.value);
    if (!Number.isFinite(v)) {
      this.currentAmount.set(0);
      return;
    }
    // clamp simple
    const clamped = Math.max(0, Math.min(Math.floor(v), 100000));
    this.currentAmount.set(clamped);
    this.repaint();
  }

  selectAmount(v: number): void {
    this.currentAmount.set(v);
    this.repaint();
  }

  submitDonation(): void {
    if (this.paying) return;

    const amount = Number(this.currentAmount());
    if (!Number.isFinite(amount) || amount < 1) {
      this.error = 'Veuillez saisir un montant (minimum 1€).';
      this.repaint();
      return;
    }

    this.paying = true;
    this.error = '';
    this.repaint();

    this.api
      .createCheckoutSession({
        amount: Math.floor(amount), // euros
        paymentMethod: 'CARD',
      })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.paying = false;
          this.repaint();
        })
      )
      .subscribe({
        next: (res) => {
          if (!this.isBrowser || !res?.checkoutUrl) {
            this.error = 'Impossible de démarrer le paiement.';
            this.repaint();
            return;
          }
          window.location.href = res.checkoutUrl;
        },
        error: (err) => {
          console.error(err);
          this.error = err?.error?.message ?? 'Paiement impossible. Réessayez.';
          this.repaint();
        },
      });
  }
}
