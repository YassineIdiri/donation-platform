import { TestBed, ComponentFixture } from '@angular/core/testing';
import { PLATFORM_ID } from '@angular/core';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { Subject, of, throwError } from 'rxjs';

import { AdminReceiptsComponent } from './admin-receipts.component';
import {
  AdminReceiptsService,
  ReceiptAdminRow,
} from '../../../core/services/admin-receipts.service';

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

type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};

describe('AdminReceiptsComponent (Vitest)', () => {
  let fixture: ComponentFixture<AdminReceiptsComponent>;
  let component: AdminReceiptsComponent;

  let apiMock: {
    list: ReturnType<typeof vi.fn>;
    resend: ReturnType<typeof vi.fn>;
    downloadPdf: ReturnType<typeof vi.fn>;
  };

  function makePage(items: ReceiptAdminRow[], opts?: Partial<PageResponse<ReceiptAdminRow>>): PageResponse<ReceiptAdminRow> {
    return {
      items,
      page: 0,
      size: 20,
      totalItems: items.length,
      totalPages: 1,
      ...opts,
    };
  }

  const row = (overrides?: Partial<ReceiptAdminRow>): ReceiptAdminRow => ({
    id: 'r1',
    donationId: 'd1',
    receiptNumber: 123,
    status: 'REQUESTED',
    requestedAt: '2026-01-01T10:00:00Z',
    issuedAt: null,
    emailMasked: 'a***@mail.com',
    ...overrides,
  });

  beforeEach(async () => {
    apiMock = {
      list: vi.fn(),
      resend: vi.fn(),
      downloadPdf: vi.fn(),
    };

    // Defaults safe
    apiMock.list.mockReturnValue(of(makePage([])));
    apiMock.resend.mockReturnValue(of({}));
    apiMock.downloadPdf.mockReturnValue(of(new Blob([new Uint8Array([1, 2, 3])], { type: 'application/pdf' })));

    await TestBed.configureTestingModule({
      imports: [AdminReceiptsComponent],
      providers: [
        { provide: AdminReceiptsService, useValue: apiMock as unknown as AdminReceiptsService },
        { provide: PLATFORM_ID, useValue: 'browser' },
      ],
    })
      .overrideComponent(AdminReceiptsComponent, { set: { template: '' } })
      .compileComponents();

    fixture = TestBed.createComponent(AdminReceiptsComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    vi.clearAllMocks();
    vi.useRealTimers();
    TestBed.resetTestingModule();
  });

  it('should be created with an empty page by default', () => {
    expect(component).toBeTruthy();
    expect(component.loading).toBe(false);
    expect(component.error).toBe(null);
    expect(component.toast).toBe(null);
    expect(component.busyId).toBe(null);
    expect(component.pageData.items).toEqual([]);
    expect(component.pageData.totalItems).toBe(0);
  });

  describe('ngOnInit', () => {
    it('should call load() on the browser side', () => {
      const loadSpy = vi.spyOn(component, 'load').mockImplementation(() => {});
      component.ngOnInit();
      expect(loadSpy).toHaveBeenCalledTimes(1);
    });

    it("should not call load() on the server side", async () => {
      TestBed.resetTestingModule();

      apiMock = {
        list: vi.fn().mockReturnValue(of(makePage([]))),
        resend: vi.fn().mockReturnValue(of({})),
        downloadPdf: vi.fn().mockReturnValue(of(new Blob())),
      };

      await TestBed.configureTestingModule({
        imports: [AdminReceiptsComponent],
        providers: [
          { provide: AdminReceiptsService, useValue: apiMock as unknown as AdminReceiptsService },
          { provide: PLATFORM_ID, useValue: 'server' },
        ],
      })
        .overrideComponent(AdminReceiptsComponent, { set: { template: '' } })
        .compileComponents();

      const fx = TestBed.createComponent(AdminReceiptsComponent);
      const cmp = fx.componentInstance;

      const loadSpy = vi.spyOn(cmp, 'load').mockImplementation(() => {});
      cmp.ngOnInit();

      expect(loadSpy).not.toHaveBeenCalled();
    });
  });

  describe('load', () => {
    it('success: loading=true then pageData then loading=false', () => {
      const stream$ = new Subject<PageResponse<ReceiptAdminRow>>();
      apiMock.list.mockReturnValue(stream$.asObservable());

      component.status = 'ALL';
      component.page = 0;
      component.size = 20;

      component.load();
      expect(component.loading).toBe(true);
      expect(component.error).toBe(null);

      const page = makePage([row({ id: 'r1' }), row({ id: 'r2', donationId: 'd2' })], { totalPages: 3 });
      stream$.next(page);
      stream$.complete();

      expect(apiMock.list).toHaveBeenCalledWith({
        page: 0,
        size: 20,
        status: '', // ALL => ''
      });

      expect(component.pageData.items.length).toBe(2);
      expect(component.pageData.totalPages).toBe(3);
      expect(component.loading).toBe(false);
      expect(component.error).toBe(null);
    });

    it('should send status if status != ALL', () => {
      const stream$ = new Subject<PageResponse<ReceiptAdminRow>>();
      apiMock.list.mockReturnValue(stream$.asObservable());

      component.status = 'ISSUED';
      component.page = 2;
      component.size = 10;

      component.load();

      expect(apiMock.list).toHaveBeenCalledWith({
        page: 2,
        size: 10,
        status: 'ISSUED',
      });

      stream$.next(makePage([]));
      stream$.complete();
      expect(component.loading).toBe(false);
    });

    it('if API returns undefined/null, should fallback to emptyPage()', () => {
      const stream$ = new Subject<any>();
      apiMock.list.mockReturnValue(stream$.asObservable());

      component.load();
      stream$.next(undefined);
      stream$.complete();

      expect(component.pageData.items).toEqual([]);
      expect(component.pageData.totalItems).toBe(0);
      expect(component.loading).toBe(false);
      expect(component.error).toBe(null);
    });

    it("error: should set error + reset emptyPage + loading=false", () => {
      const stream$ = new Subject<PageResponse<ReceiptAdminRow>>();
      apiMock.list.mockReturnValue(stream$.asObservable());

      component.pageData = makePage([row({ id: 'x' })], { totalItems: 1, totalPages: 2 });

      component.load();
      expect(component.loading).toBe(true);

      stream$.error(new Error('boom'));

      expect(component.loading).toBe(false);
      expect(component.error).toBe('Unable to load receipts.');
      expect(component.pageData.items).toEqual([]);
      expect(component.pageData.totalItems).toBe(0);
    });

    it('takeUntil: after ngOnDestroy, a next should not change pageData', () => {
      const stream$ = new Subject<PageResponse<ReceiptAdminRow>>();
      apiMock.list.mockReturnValue(stream$.asObservable());

      component.load();
      const before = component.pageData;

      component.ngOnDestroy();

      stream$.next(makePage([row({ id: 'should-not-apply' })]));
      expect(component.pageData).toBe(before);
    });
  });

  describe('pagination', () => {
    it('nextPage() does nothing if page+1 >= totalPages', () => {
      component.page = 0;
      component.pageData = makePage([], { totalPages: 1 });

      const loadSpy = vi.spyOn(component, 'load').mockImplementation(() => {});
      component.nextPage();

      expect(component.page).toBe(0);
      expect(loadSpy).not.toHaveBeenCalled();
    });

    it('nextPage() increments page and calls load if page+1 < totalPages', () => {
      component.page = 0;
      component.pageData = makePage([], { totalPages: 3 });

      const loadSpy = vi.spyOn(component, 'load').mockImplementation(() => {});
      component.nextPage();

      expect(component.page).toBe(1);
      expect(loadSpy).toHaveBeenCalledTimes(1);
    });

    it('prevPage() does nothing if page=0', () => {
      component.page = 0;

      const loadSpy = vi.spyOn(component, 'load').mockImplementation(() => {});
      component.prevPage();

      expect(component.page).toBe(0);
      expect(loadSpy).not.toHaveBeenCalled();
    });

    it('prevPage() decrements page and calls load if page>0', () => {
      component.page = 2;

      const loadSpy = vi.spyOn(component, 'load').mockImplementation(() => {});
      component.prevPage();

      expect(component.page).toBe(1);
      expect(loadSpy).toHaveBeenCalledTimes(1);
    });
  });

  describe('rows / filtered', () => {
    beforeEach(() => {
      component.pageData = makePage([
        row({ id: '1', donationId: 'don-aaa', emailMasked: 'john***@mail.com', status: 'ISSUED' }),
        row({ id: '2', donationId: 'don-bbb', emailMasked: 'alice***@mail.com', status: 'REQUESTED' }),
        row({ id: '10', donationId: 'don-ccc', emailMasked: 'bob***@mail.com', status: 'FAILED' }),
      ]);
    });

    it('rows returns items (or [])', () => {
      expect(component.rows.map(r => r.id)).toEqual(['1', '2', '10']);
    });

    it('filtered filters by q (id/donationId/emailMasked) and duplicates the list (2x) with clones', () => {
      component.status = 'ALL';

      component.q = '10';
      const res = component.filtered;
      expect(res.map(r => r.id)).toEqual(['10', '10']);
      expect(res[0]).not.toBe(res[1]); // clones

      component.q = 'ALICE';
      expect(component.filtered.map(r => r.id)).toEqual(['2', '2']);

      component.q = 'don-aaa';
      expect(component.filtered.map(r => r.id)).toEqual(['1', '1']);
    });

    it('filtered filters by status if status != ALL', () => {
      component.q = '';
      component.status = 'ISSUED';
      expect(component.filtered.map(r => r.id)).toEqual(['1', '1']);

      component.status = 'FAILED';
      expect(component.filtered.map(r => r.id)).toEqual(['10', '10']);
    });

    it('filtered combines q + status', () => {
      component.status = 'REQUESTED';
      component.q = 'alice';
      expect(component.filtered.map(r => r.id)).toEqual(['2', '2']);
    });
  });

  describe('receiptLabel', () => {
    it('CERFA-xxxxxx if receiptNumber != null', () => {
      expect(component.receiptLabel(row({ id: 'abc', receiptNumber: 7 }))).toBe('CERFA-000007');
    });

    it('DRAFT-id if receiptNumber == null', () => {
      expect(component.receiptLabel(row({ id: 'abc', receiptNumber: null }))).toBe('DRAFT-abc');
    });
  });

  describe('badgeClass', () => {
    it('returns the correct class depending on status', () => {
      expect(component.badgeClass('ISSUED')).toContain('bg-emerald-100');
      expect(component.badgeClass('FAILED')).toContain('bg-red-100');
      expect(component.badgeClass('REQUESTED')).toContain('bg-amber-100');
    });
  });

  describe('resend', () => {
    it('does nothing if busyId is already set', () => {
      component.busyId = 'already';
      const loadSpy = vi.spyOn(component, 'load').mockImplementation(() => {});
      component.resend('r1');

      expect(apiMock.resend).not.toHaveBeenCalled();
      expect(loadSpy).not.toHaveBeenCalled();
    });

    it('success: sets busyId, toast, calls load, then clears toast after 2500ms', async () => {
      vi.useFakeTimers();

      const resend$ = new Subject<any>();
      apiMock.resend.mockReturnValue(resend$.asObservable());
      const loadSpy = vi.spyOn(component, 'load').mockImplementation(() => {});

      component.resend('r1');

      expect(component.busyId).toBe('r1');
      expect(component.error).toBe(null);
      expect(component.toast).toBe(null);
      expect(apiMock.resend).toHaveBeenCalledWith('r1');

      // success
      resend$.next({});
      resend$.complete();

      // finalize => busyId cleared, next => toast + load()
      expect(component.busyId).toBe(null);
      expect(component.toast).toBe('Receipt resent ✅');
      expect(loadSpy).toHaveBeenCalledTimes(1);

      // timeout 2500ms => toast cleared
      vi.advanceTimersByTime(2500);
      await vi.runOnlyPendingTimersAsync();
      expect(component.toast).toBe(null);
    });

    it('error: sets error and clears busyId (finalize)', () => {
      const resend$ = new Subject<any>();
      apiMock.resend.mockReturnValue(resend$.asObservable());

      component.resend('r1');
      expect(component.busyId).toBe('r1');

      resend$.error(new Error('boom'));

      expect(component.busyId).toBe(null);
      expect(component.error).toBe('Unable to resend the receipt.');
    });
  });

  describe('downloadPdf', () => {
    it('does nothing on the server side', async () => {
      TestBed.resetTestingModule();

      apiMock = {
        list: vi.fn().mockReturnValue(of(makePage([]))),
        resend: vi.fn().mockReturnValue(of({})),
        downloadPdf: vi.fn().mockReturnValue(of(new Blob())),
      };

      await TestBed.configureTestingModule({
        imports: [AdminReceiptsComponent],
        providers: [
          { provide: AdminReceiptsService, useValue: apiMock as unknown as AdminReceiptsService },
          { provide: PLATFORM_ID, useValue: 'server' },
        ],
      })
        .overrideComponent(AdminReceiptsComponent, { set: { template: '' } })
        .compileComponents();

      const fx = TestBed.createComponent(AdminReceiptsComponent);
      const cmp = fx.componentInstance;

      cmp.downloadPdf(row({ id: 'r1' }));
      expect(apiMock.downloadPdf).not.toHaveBeenCalled();
    });

    it('success: creates a link, clicks it, then revokeObjectURL', () => {
      const blob = new Blob([new Uint8Array([1])], { type: 'application/pdf' });
      apiMock.downloadPdf.mockReturnValue(of(blob));

      const createUrlSpy = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:mock');
      const revokeSpy = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {});

      const clickSpy = vi.fn();
      const createElSpy = vi.spyOn(document, 'createElement').mockImplementation((tagName: any) => {
        if (tagName === 'a') {
          return { href: '', download: '', click: clickSpy } as any;
        }
        return document.createElement(tagName);
      });

      component.downloadPdf(row({ id: 'r77', receiptNumber: 12 }));

      expect(apiMock.downloadPdf).toHaveBeenCalledWith('r77');
      expect(createUrlSpy).toHaveBeenCalledWith(blob);
      expect(clickSpy).toHaveBeenCalledTimes(1);
      expect(revokeSpy).toHaveBeenCalledWith('blob:mock');

      createElSpy.mockRestore();
    });

    it('error: should set error = "Download failed."', () => {
      apiMock.downloadPdf.mockReturnValue(throwError(() => new Error('boom')));

      component.downloadPdf(row({ id: 'r1' }));

      expect(component.error).toBe('Download failed.');
    });
  });
});
