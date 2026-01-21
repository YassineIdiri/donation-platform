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
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { PublicApiService, DonationResponse, ReceiptResponse } from '../../../core/services/public-api.service';

type ReceiptForm = {
  want: FormControl<boolean>;
  email: FormControl<string>;
  fullName: FormControl<string>;
  address: FormControl<string>;
  consent: FormControl<boolean>;
};

@Component({
  selector: 'app-thanks',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './thanks.component.html',
})
export class ThanksComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly isBrowser: boolean;

  loading = true;
  refreshing = false;
  sending = false;

  error = '';
  toast = '';

  donationId = '';
  donation: DonationResponse | null = null;
  receipt: ReceiptResponse | null = null;

  form: FormGroup<ReceiptForm>;

  constructor(
    private readonly api: PublicApiService,
    private readonly route: ActivatedRoute,
    private readonly fb: FormBuilder,
    private readonly cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);

   this.form = this.fb.group<ReceiptForm>({
     want: this.fb.nonNullable.control(false),
     email: this.fb.nonNullable.control('', [Validators.email, Validators.maxLength(254)]),
     fullName: this.fb.nonNullable.control('', [Validators.maxLength(180)]),
     address: this.fb.nonNullable.control('', [Validators.maxLength(1000)]),
     consent: this.fb.nonNullable.control(false),
   });
  }

  ngOnInit(): void {
    if (!this.isBrowser) return;

    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((m) => {
      this.donationId = m.get('donationId') ?? '';
      if (!this.donationId) {
        this.error = 'Référence de don manquante.';
        this.loading = false;
        this.repaint();
        return;
      }
      this.loadDonation();
    });

    this.form.get('want')?.valueChanges.pipe(takeUntil(this.destroy$)).subscribe((v) => {
      const email = this.form.get('email');
      const fullName = this.form.get('fullName');
      const address = this.form.get('address');
      const consent = this.form.get('consent');

      if (v) {
        email?.addValidators([Validators.required]);
        fullName?.addValidators([Validators.required, Validators.minLength(2)]);
        address?.addValidators([Validators.required, Validators.minLength(5)]);
        consent?.addValidators([Validators.requiredTrue]);
      } else {
        email?.removeValidators([Validators.required]);
        fullName?.removeValidators([Validators.required, Validators.minLength(2)]);
        address?.removeValidators([Validators.required, Validators.minLength(5)]);
        consent?.removeValidators([Validators.requiredTrue]);
        consent?.setValue(false, { emitEvent: false });
      }

      email?.updateValueAndValidity({ emitEvent: false });
      fullName?.updateValueAndValidity({ emitEvent: false });
      address?.updateValueAndValidity({ emitEvent: false });
      consent?.updateValueAndValidity({ emitEvent: false });

      this.repaint();
    });
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

  loadDonation(): void {
    this.loading = true;
    this.error = '';
    this.repaint();

    this.api
      .getDonation(this.donationId)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
          this.repaint();
        })
      )
      .subscribe({
        next: (d) => {
          this.donation = d;
          this.repaint();
        },
        error: (err) => {
          console.error(err);
          this.error = 'Impossible de récupérer le statut du don.';
          this.repaint();
        },
      });
  }

  refreshStatus(): void {
    if (this.refreshing) return;
    this.refreshing = true;
    this.error = '';
    this.repaint();

    this.api
      .getDonation(this.donationId)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.refreshing = false;
          this.repaint();
        })
      )
      .subscribe({
        next: (d) => {
          this.donation = d;
          this.showToast('Statut mis à jour ✅');
          this.repaint();
        },
        error: (err) => {
          console.error(err);
          this.error = 'Actualisation impossible.';
          this.repaint();
        },
      });
  }

  submitReceipt(): void {
    if (this.sending) return;

    if (!this.form.get('want')?.value) {
      this.showToast('Reçu fiscal non demandé.');
      return;
    }

    if (!this.donation || this.donation.status !== 'PAID') {
      this.error = 'Le paiement doit être confirmé avant l’émission du reçu fiscal.';
      this.repaint();
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.repaint();
      return;
    }

    const raw = this.form.getRawValue();

    this.sending = true;
    this.error = '';
    this.repaint();

    this.api
      .requestReceipt({
        donationId: this.donationId,
        email: raw.email.trim(),
        fullName: raw.fullName.trim(),
        address: raw.address.trim(),
      })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.sending = false;
          this.repaint();
        })
      )
      .subscribe({
        next: (r) => {
          this.receipt = r;
          this.showToast('Demande envoyée ✅ Vous recevrez le reçu par email.');
          this.repaint();
        },
        error: (err) => {
          console.error(err);
          this.error = err?.error?.message ?? 'Envoi du reçu fiscal impossible.';
          this.repaint();
        },
      });
  }

  euros(): string {
    if (!this.donation) return '';
    return `${(this.donation.amountCents / 100).toFixed(2)} ${this.donation.currency}`;
  }

  isPaid(): boolean {
    return this.donation?.status === 'PAID';
  }
}
