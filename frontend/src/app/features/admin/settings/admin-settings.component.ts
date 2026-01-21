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
  FormBuilder,
  ReactiveFormsModule,
  Validators,
  FormArray,
  FormControl,
  FormGroup,
} from '@angular/forms';
import { Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';

import { AdminSettingsService, PublicUiSettings } from '../../../core/services/admin-settings.service';
import { ui, Ui } from '../../../core/utils/zoneless-ui';

const DEFAULT_AMOUNTS = [5, 20, 30, 50, 100];
const FIXED_COUNT = 5;

type SettingsForm = {
  title: FormControl<string>;
  primaryColor: FormControl<string>;
  suggestedAmounts: FormArray<FormControl<number>>;
};

@Component({
  selector: 'app-admin-settings',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-settings.component.html',
})
export class AdminSettingsComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly isBrowser: boolean;
  private readonly ui: Ui;

  loading = true;
  saving = false;
  toast: string | null = null;
  error: string | null = null;

  form: FormGroup<SettingsForm>;

  constructor(
    private readonly api: AdminSettingsService,
    private readonly fb: FormBuilder,
    private readonly cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
    this.ui = ui(this.cdr);

    // ✅ init form
    this.form = this.fb.group<SettingsForm>({
      title: this.fb.nonNullable.control('', [
        Validators.required,
        Validators.maxLength(80),
      ]),
      primaryColor: this.fb.nonNullable.control('#10B981', [
        Validators.required,
        Validators.pattern(/^#[0-9A-Fa-f]{6}$/),
      ]),
      suggestedAmounts: this.fb.array<FormControl<number>>([]),
    });

    // ✅ Toujours 5 champs
    for (let i = 0; i < FIXED_COUNT; i++) {
      const v = DEFAULT_AMOUNTS[i] ?? 10;
      this.suggestedAmounts.push(
        this.fb.nonNullable.control(v, [
          Validators.required,
          Validators.min(1),
        ])
      );
    }

    // ✅ Synchronisation couleur: normalise dès qu'on change
    this.form.controls.primaryColor.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((v) => {
        const normalized = normalizeHex6(v);
        if (normalized !== v) {
          this.form.controls.primaryColor.setValue(normalized, { emitEvent: false });
          this.ui.repaint();
        }
      });
  }

  ngOnInit(): void {
    if (!this.isBrowser) return;
    this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get suggestedAmounts(): FormArray<FormControl<number>> {
    return this.form.controls.suggestedAmounts;
  }

  private load(): void {
    this.ui.set(() => {
      this.loading = true;
      this.error = null;
      this.toast = null;
    });

    this.api
      .getPublicUi()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.ui.set(() => (this.loading = false))),
        this.ui.pipeRepaint()
      )
      .subscribe({
        next: (s: PublicUiSettings) => {
          const title = s?.title ?? '';
          const color = normalizeHex6(s?.primaryColor ?? '#10B981');

          // ✅ Toujours 5 valeurs: pad/trim
          const incoming = Array.isArray(s?.suggestedAmounts) ? s.suggestedAmounts : [];
          const cleaned = incoming
            .map((x) => Number(x))
            .filter((x) => Number.isFinite(x) && x > 0);

          const fixed: number[] = [];
          for (let i = 0; i < FIXED_COUNT; i++) {
            fixed.push(cleaned[i] ?? DEFAULT_AMOUNTS[i] ?? 10);
          }

          this.ui.set(() => {
            this.form.patchValue({
              title,
              primaryColor: color,
            });

            // ✅ Remplit exactement les 5 controls existants (pas clear/push)
            for (let i = 0; i < FIXED_COUNT; i++) {
              this.suggestedAmounts.at(i).setValue(fixed[i], { emitEvent: false });
            }
          });
        },
        error: () => {
          this.ui.set(() => (this.error = 'Impossible de charger les réglages.'));
        },
      });
  }

  // ✅ Optionnel: helper pour la vue
  amountCtrl(i: number): FormControl<number> {
    return this.suggestedAmounts.at(i) as FormControl<number>;
  }

  save(): void {
    if (this.saving) return;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.ui.repaint();
      return;
    }

    this.ui.set(() => {
      this.saving = true;
      this.error = null;
      this.toast = null;
    });

    const raw = this.form.getRawValue();

    const payload: PublicUiSettings = {
      title: String(raw.title ?? '').trim(),
      primaryColor: normalizeHex6(String(raw.primaryColor ?? '')),
      // ✅ Toujours 5 montants
      suggestedAmounts: (raw.suggestedAmounts ?? [])
        .map((x) => Number(x))
        .map((x) => (Number.isFinite(x) && x > 0 ? Math.floor(x) : 1))
        .slice(0, FIXED_COUNT),
    };

    // Sécurité: si jamais un champ est vide, on pad quand même
    while (payload.suggestedAmounts.length < FIXED_COUNT) {
      payload.suggestedAmounts.push(DEFAULT_AMOUNTS[payload.suggestedAmounts.length] ?? 10);
    }

    this.api
      .updatePublicUi(payload)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.ui.set(() => (this.saving = false))),
        this.ui.pipeRepaint()
      )
      .subscribe({
        next: () => {
          this.ui.set(() => (this.toast = 'Réglages enregistrés ✅'));
          if (this.isBrowser) {
            window.setTimeout(() => this.ui.set(() => (this.toast = null)), 2500);
          }
        },
        error: () => this.ui.set(() => (this.error = 'Enregistrement impossible.')),
      });
  }
}

/** Normalise en "#RRGGBB" uppercase, fallback si invalide */
function normalizeHex6(v: string | null | undefined): string {
  const fallback = '#10B981';
  if (!v) return fallback;

  let s = String(v).trim();
  if (!s.startsWith('#')) s = `#${s}`;

  // garde uniquement # + hex
  s = s.replace(/[^#0-9a-fA-F]/g, '');

  // "#ABC" -> "#AABBCC"
  if (/^#[0-9a-fA-F]{3}$/.test(s)) {
    const r = s[1], g = s[2], b = s[3];
    s = `#${r}${r}${g}${g}${b}${b}`;
  }

  // tronque à 7
  s = s.slice(0, 7);

  if (!/^#[0-9a-fA-F]{6}$/.test(s)) return fallback;
  return s.toUpperCase();
}
