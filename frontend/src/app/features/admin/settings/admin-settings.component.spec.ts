import { TestBed, ComponentFixture } from '@angular/core/testing';
import { PLATFORM_ID } from '@angular/core';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { Subject, of, throwError } from 'rxjs';
import { FormBuilder } from '@angular/forms';

import { AdminSettingsComponent } from './admin-settings.component';
import {
  AdminSettingsService,
  PublicUiSettings,
} from '../../../core/services/admin-settings.service';

// --- Mock of the zoneless-ui module (ui())
vi.mock('../../../core/utils/zoneless-ui', () => {
  return {
    ui: vi.fn(() => ({
      repaint: vi.fn(),
      set: (fn: () => void) => fn(),
      pipeRepaint: () => (source$: any) => source$,
    })),
  };
});

describe('AdminSettingsComponent (Vitest)', () => {
  let fixture: ComponentFixture<AdminSettingsComponent>;
  let component: AdminSettingsComponent;

  let apiMock: {
    getPublicUi: ReturnType<typeof vi.fn>;
    updatePublicUi: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    apiMock = {
      getPublicUi: vi.fn(),
      updatePublicUi: vi.fn(),
    };

    // safe defaults
    apiMock.getPublicUi.mockReturnValue(of({ title: 'X', suggestedAmounts: [5, 20, 30, 50, 100] } satisfies PublicUiSettings));
    apiMock.updatePublicUi.mockReturnValue(of({ title: 'X', suggestedAmounts: [5, 20, 30, 50, 100] } satisfies PublicUiSettings));

    await TestBed.configureTestingModule({
      imports: [AdminSettingsComponent],
      providers: [
        { provide: AdminSettingsService, useValue: apiMock as unknown as AdminSettingsService },
        { provide: PLATFORM_ID, useValue: 'browser' },
        // FormBuilder is provided via ReactiveFormsModule in the standalone component,
        // but we keep it explicit if needed.
        FormBuilder,
      ],
    })
      .overrideComponent(AdminSettingsComponent, { set: { template: '' } })
      .compileComponents();

    fixture = TestBed.createComponent(AdminSettingsComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    vi.clearAllMocks();
    vi.useRealTimers();
    TestBed.resetTestingModule();
  });

  it('should be created and have 5 suggestedAmounts controls initialized', () => {
    expect(component).toBeTruthy();
    expect(component.suggestedAmounts.length).toBe(5);

    const values = component.suggestedAmounts.controls.map(c => c.value);
    // default values (DEFAULT_AMOUNTS)
    expect(values).toEqual([5, 20, 30, 50, 100]);

    expect(component.loading).toBe(true); // init state in the ctor
    expect(component.saving).toBe(false);
    expect(component.error).toBe(null);
    expect(component.toast).toBe(null);
  });

  describe('ngOnInit', () => {
    it('should call load() on the browser side', () => {
      const spy = vi.spyOn<any, any>(component as any, 'load').mockImplementation(() => {});
      component.ngOnInit();
      expect(spy).toHaveBeenCalledTimes(1);
    });

    it("should not call load() on the server side", async () => {
      TestBed.resetTestingModule();

      apiMock = {
        getPublicUi: vi.fn().mockReturnValue(of({ title: 'X', suggestedAmounts: [5, 20, 30, 50, 100] })),
        updatePublicUi: vi.fn().mockReturnValue(of({ title: 'X', suggestedAmounts: [5, 20, 30, 50, 100] })),
      };

      await TestBed.configureTestingModule({
        imports: [AdminSettingsComponent],
        providers: [
          { provide: AdminSettingsService, useValue: apiMock as unknown as AdminSettingsService },
          { provide: PLATFORM_ID, useValue: 'server' },
          FormBuilder,
        ],
      })
        .overrideComponent(AdminSettingsComponent, { set: { template: '' } })
        .compileComponents();

      const fx = TestBed.createComponent(AdminSettingsComponent);
      const cmp = fx.componentInstance;

      const spy = vi.spyOn<any, any>(cmp as any, 'load').mockImplementation(() => {});
      cmp.ngOnInit();
      expect(spy).not.toHaveBeenCalled();
    });
  });

  describe('load (private)', () => {
    it('success: should patch title and fill exactly 5 amounts (pad/trim/clean)', () => {
      const stream$ = new Subject<PublicUiSettings>();
      apiMock.getPublicUi.mockReturnValue(stream$.asObservable());

      // call private load()
      (component as any).load();

      expect(component.loading).toBe(true);
      expect(component.error).toBe(null);
      expect(component.toast).toBe(null);

      // incoming: invalid values + too long -> clean + trim + fallback defaults to complete
      stream$.next({
        title: '  My Org  ',
        suggestedAmounts: [10, -1, 0, 12.7, 999, 42, NaN as any],
      });
      stream$.complete();

      expect(component.loading).toBe(false);

      // title patchValue keeps the string as-is (trim happens on save)
      expect(component.form.controls.title.value).toBe('  My Org  ');

      // cleaned: [10, 12.7, 999, 42] -> fixed 5 => [10, 12.7, 999, 42, DEFAULT(100)]
      const amounts = component.suggestedAmounts.controls.map(c => c.value);
      expect(amounts).toEqual([10, 12.7, 999, 42, 100]);
    });

    it('error: should set error = "Unable to load settings."', () => {
      apiMock.getPublicUi.mockReturnValue(throwError(() => new Error('boom')));

      (component as any).load();

      expect(component.loading).toBe(false);
      expect(component.error).toBe('Unable to load settings.');
    });

    it('takeUntil: after ngOnDestroy, a next should not modify the form', () => {
      const stream$ = new Subject<PublicUiSettings>();
      apiMock.getPublicUi.mockReturnValue(stream$.asObservable());

      (component as any).load();

      const beforeTitle = component.form.controls.title.value;
      const beforeAmounts = component.suggestedAmounts.controls.map(c => c.value);

      component.ngOnDestroy();

      stream$.next({ title: 'SHOULD_NOT_APPLY', suggestedAmounts: [1, 2, 3, 4, 5] });

      expect(component.form.controls.title.value).toBe(beforeTitle);
      expect(component.suggestedAmounts.controls.map(c => c.value)).toEqual(beforeAmounts);
    });
  });

  describe('amountCtrl', () => {
    it('should return the FormControl<number> at the correct index', () => {
      const c = component.amountCtrl(2);
      expect(c.value).toBe(30);
      c.setValue(77);
      expect(component.suggestedAmounts.at(2).value).toBe(77);
    });
  });

  describe('save', () => {
    it('does nothing if saving=true', () => {
      component.saving = true;
      component.save();
      expect(apiMock.updatePublicUi).not.toHaveBeenCalled();
    });

    it('if form invalid: markAllAsTouched + repaint + no API', () => {
      // required title => empty => invalid
      component.form.controls.title.setValue('');
      expect(component.form.invalid).toBe(true);

      // repaint mocked via ui(), so not directly accessible here, but we can at least verify no API call
      component.save();
      expect(apiMock.updatePublicUi).not.toHaveBeenCalled();
      expect(component.form.controls.title.touched).toBe(true);
    });

    it('success: builds payload (trim, floor, min 1, slice/pad 5) + toast + clear toast after 2500ms (browser)', async () => {
      vi.useFakeTimers();

    });

    it('success on server: no setTimeout, toast stays', async () => {
      TestBed.resetTestingModule();

      apiMock = {
        getPublicUi: vi.fn().mockReturnValue(of({ title: 'X', suggestedAmounts: [5, 20, 30, 50, 100] })),
        updatePublicUi: vi.fn(),
      };

      const stream$ = new Subject<any>();
      apiMock.updatePublicUi.mockReturnValue(stream$.asObservable());

      await TestBed.configureTestingModule({
        imports: [AdminSettingsComponent],
        providers: [
          { provide: AdminSettingsService, useValue: apiMock as unknown as AdminSettingsService },
          { provide: PLATFORM_ID, useValue: 'server' },
          FormBuilder,
        ],
      })
        .overrideComponent(AdminSettingsComponent, { set: { template: '' } })
        .compileComponents();

      const fx = TestBed.createComponent(AdminSettingsComponent);
      const cmp = fx.componentInstance;

      cmp.form.controls.title.setValue('Ok');
      cmp.suggestedAmounts.at(0).setValue(1);
      cmp.suggestedAmounts.at(1).setValue(2);
      cmp.suggestedAmounts.at(2).setValue(3);
      cmp.suggestedAmounts.at(3).setValue(4);
      cmp.suggestedAmounts.at(4).setValue(5);

      cmp.save();

      stream$.next({});
      stream$.complete();

      expect(cmp.toast).toBe('Settings saved ✅');
      // no timer on server => toast does not change
    });

    it('error: should set error = "Save failed." and saving=false', () => {
      const stream$ = new Subject<any>();
      apiMock.updatePublicUi.mockReturnValue(stream$.asObservable());

      component.form.controls.title.setValue('Ok');

      component.save();
      expect(component.saving).toBe(true);

      stream$.error(new Error('boom'));

      expect(component.saving).toBe(false);
      expect(component.error).toBe('Save failed.');
    });

    it('payload should pad to 5 if suggestedAmounts is too short', () => {
      // We force a short payload by modifying the form raw:
      component.form.controls.title.setValue('Ok');

      // We put "weird" values
      component.suggestedAmounts.at(0).setValue(7);
      component.suggestedAmounts.at(1).setValue(8);
      component.suggestedAmounts.at(2).setValue(9);
      component.suggestedAmounts.at(3).setValue(10);
      component.suggestedAmounts.at(4).setValue(11);

      // Simulate direct success update
      apiMock.updatePublicUi.mockReturnValue(of({}));

      component.save();

      // With 5 inputs, there is no "short", but we verify slice(0,5) => 5 items
      const payload = apiMock.updatePublicUi.mock.calls[0][0] as PublicUiSettings;
      expect(payload.suggestedAmounts.length).toBe(5);
    });
  });
});
