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

// Removal of primaryColor from the type
type SettingsForm = {
  title: FormControl<string>;
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

    // Removal of the primaryColor control
    this.form = this.fb.group<SettingsForm>({
      title: this.fb.nonNullable.control('', [
        Validators.required,
        Validators.maxLength(80),
      ]),
      suggestedAmounts: this.fb.array<FormControl<number>>([]),
    });

    for (let i = 0; i < FIXED_COUNT; i++) {
      const v = DEFAULT_AMOUNTS[i] ?? 10;
      this.suggestedAmounts.push(
        this.fb.nonNullable.control(v, [
          Validators.required,
          Validators.min(1),
        ])
      );
    }
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

          // ✅ Always 5 values: pad/trim
          const incoming = Array.isArray(s?.suggestedAmounts) ? s.suggestedAmounts : [];
          const cleaned = incoming
            .map((x) => Number(x))
            .filter((x) => Number.isFinite(x) && x > 0);

          const fixed: number[] = [];
          for (let i = 0; i < FIXED_COUNT; i++) {
            fixed.push(cleaned[i] ?? DEFAULT_AMOUNTS[i] ?? 10);
          }

          this.ui.set(() => {
            // No more patchValue on primaryColor
            this.form.patchValue({
              title,
            });

            // ✅ Fills exactly the 5 existing controls
            for (let i = 0; i < FIXED_COUNT; i++) {
              this.suggestedAmounts.at(i).setValue(fixed[i], { emitEvent: false });
            }
          });
        },
        error: () => {
          this.ui.set(() => (this.error = 'Unable to load settings.'));
        },
      });
  }

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

    // Removal of primaryColor from the payload
    const payload: PublicUiSettings = {
      title: String(raw.title ?? '').trim(),
      suggestedAmounts: (raw.suggestedAmounts ?? [])
        .map((x) => Number(x))
        .map((x) => (Number.isFinite(x) && x > 0 ? Math.floor(x) : 1))
        .slice(0, FIXED_COUNT),
    };

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
          this.ui.set(() => (this.toast = 'Settings saved ✅'));
          if (this.isBrowser) {
            window.setTimeout(() => this.ui.set(() => (this.toast = null)), 2500);
          }
        },
        error: () => this.ui.set(() => (this.error = 'Save failed.')),
      });
  }
}
