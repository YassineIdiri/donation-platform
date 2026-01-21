import {
  Component,
  Inject,
  OnInit,
  OnDestroy,
  ChangeDetectorRef,
  PLATFORM_ID,
} from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  Validators,
  FormGroup,
  FormControl,
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { PublicApiService, SettingsResponse } from '../../../core/services/public-api.service';

type DonateForm = {
  amount: FormControl<number | null>;
  email: FormControl<string>;
  consent: FormControl<boolean>;
};

@Component({
  selector: 'app-donate',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './donate.component.html',
})
export class Donate implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly isBrowser: boolean;

  loading = true;
  paying = false;

  error = '';
  toast = '';

  settings: SettingsResponse | null = null;
  suggested: number[] = [10, 20, 50, 100];

  form: FormGroup<DonateForm>;

  constructor(
    private readonly api: PublicApiService,
    private readonly fb: FormBuilder,
    private readonly cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);

    this.form = this.fb.group<DonateForm>({
      amount: this.fb.control<number | null>(20, [
        Validators.required,
        Validators.min(1),
        Validators.max(100000),
      ]),
      email: this.fb.nonNullable.control('', [
        Validators.email,
        Validators.maxLength(254),
      ]),
      consent: this.fb.nonNullable.control(false, [Validators.requiredTrue]),
    });
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
          this.suggested = (s?.suggestedAmounts?.length ? s.suggestedAmounts : [10, 20, 50, 100]).slice(0, 8);

          if (this.isBrowser && s?.primaryColor) {
            document.documentElement.style.setProperty('--primary', s.primaryColor);
          }

          const current = this.form.get('amount')?.value ?? 0;
          if (!current || current <= 0) {
            this.form.get('amount')?.setValue(this.suggested[1] ?? this.suggested[0] ?? 10);
          }

          this.repaint();
        },
        error: (err) => {
          console.error(err);
          this.error = 'Impossible de charger la page de don.';
          this.repaint();
        },
      });
  }

  selectAmount(v: number): void {
    this.form.get('amount')?.setValue(v, { emitEvent: true });
    this.repaint();
  }

  pay(): void {
    if (this.paying) return;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.repaint();
      return;
    }

    const raw = this.form.getRawValue();
    const amount = Number(raw.amount ?? 0);

    if (!Number.isFinite(amount) || amount <= 0) {
      this.error = 'Montant invalide.';
      this.repaint();
      return;
    }

    this.paying = true;
    this.error = '';
    this.repaint();

    const payload = {
      amount: Math.round(amount), // euros (int)
      email: raw.email?.trim() || undefined,
      consent: !!raw.consent,
      paymentMethod: 'CARD' as const,
    };

    this.api
      .createCheckoutSession(payload)
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
          // redirection Stripe Checkout
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
