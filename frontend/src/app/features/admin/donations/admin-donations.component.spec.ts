import { TestBed, ComponentFixture } from '@angular/core/testing';
import { PLATFORM_ID } from '@angular/core';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { Subject, of } from 'rxjs';

import { AdminDonationsComponent } from './admin-donations.component';
import {
  AdminDonationsService,
  DonationAdminRowResponse,
} from '../../../core/services/admin-donations.service';

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
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

describe('AdminDonationsComponent (Vitest)', () => {
  let fixture: ComponentFixture<AdminDonationsComponent>;
  let component: AdminDonationsComponent;

  let apiMock: {
    listDonationsPage: ReturnType<typeof vi.fn>;
    listDonationsRows: ReturnType<typeof vi.fn>;
  };

  function makePage(
    items: DonationAdminRowResponse[],
    opts?: Partial<PageResponse<DonationAdminRowResponse>>
  ): PageResponse<DonationAdminRowResponse> {
    return {
      items,
      page: 0,
      size: 20,
      totalElements: items.length,
      totalPages: 1,
      first: true,
      last: true,
      ...opts,
    };
  }

  beforeEach(async () => {
    apiMock = {
      listDonationsPage: vi.fn(),
      listDonationsRows: vi.fn(),
    };

    // default value to avoid crashes if load() is called "by accident"
    apiMock.listDonationsPage.mockReturnValue(of(makePage([])));

    await TestBed.configureTestingModule({
      imports: [AdminDonationsComponent],
      providers: [
        { provide: AdminDonationsService, useValue: apiMock as unknown as AdminDonationsService },
        { provide: PLATFORM_ID, useValue: 'browser' },
      ],
    })
      // Standalone component with templateUrl -> override to avoid depending on the HTML file
      .overrideComponent(AdminDonationsComponent, { set: { template: '' } })
      .compileComponents();

    fixture = TestBed.createComponent(AdminDonationsComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    vi.clearAllMocks();
    TestBed.resetTestingModule();
  });

  it('should create with an empty page by default', () => {
    expect(component).toBeTruthy();
    expect(component.loading).toBe(false);
    expect(component.error).toBe(null);
    expect(component.pageData.items).toEqual([]);
    expect(component.pageData.totalElements).toBe(0);
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
        listDonationsPage: vi.fn(),
        listDonationsRows: vi.fn(),
      };

      await TestBed.configureTestingModule({
        imports: [AdminDonationsComponent],
        providers: [
          { provide: AdminDonationsService, useValue: apiMock as unknown as AdminDonationsService },
          { provide: PLATFORM_ID, useValue: 'server' },
        ],
      })
        .overrideComponent(AdminDonationsComponent, { set: { template: '' } })
        .compileComponents();

      const fx = TestBed.createComponent(AdminDonationsComponent);
      const cmp = fx.componentInstance;

      const loadSpy = vi.spyOn(cmp, 'load').mockImplementation(() => {});
      cmp.ngOnInit();

      expect(loadSpy).not.toHaveBeenCalled();
    });
  });

  describe('load', () => {
    it('success: should set loading=true then update pageData then loading=false', () => {
      const stream$ = new Subject<PageResponse<DonationAdminRowResponse>>();
      apiMock.listDonationsPage.mockReturnValue(stream$.asObservable());

      component.status = 'ALL';
      component.page = 0;
      component.size = 20;

      component.load();
      expect(component.loading).toBe(true);
      expect(component.error).toBe(null);

      const page = makePage([
        {
          id: '1',
          createdAt: '2026-01-01T10:00:00Z',
          amountCents: 1000,
          currency: 'EUR',
          status: 'PAID',
          emailMasked: 'a***@x.com',
        },
      ]);

      stream$.next(page);
      stream$.complete();

      expect(apiMock.listDonationsPage).toHaveBeenCalledWith({
        page: 0,
        size: 20,
        status: undefined, // ALL => undefined
      });

      expect(component.pageData.items).toEqual(page.items);
      expect(component.loading).toBe(false);
      expect(component.error).toBe(null);
    });

    it('should send status if status != ALL', () => {
      const stream$ = new Subject<PageResponse<DonationAdminRowResponse>>();
      apiMock.listDonationsPage.mockReturnValue(stream$.asObservable());

      component.status = 'PAID';
      component.page = 3;
      component.size = 10;

      component.load();

      expect(apiMock.listDonationsPage).toHaveBeenCalledWith({
        page: 3,
        size: 10,
        status: 'PAID',
      });

      stream$.next(makePage([]));
      stream$.complete();
      expect(component.loading).toBe(false);
    });

    it('if API returns undefined/null, should fallback to emptyPage()', () => {
      const stream$ = new Subject<any>();
      apiMock.listDonationsPage.mockReturnValue(stream$.asObservable());

      component.load();
      stream$.next(undefined);
      stream$.complete();

      expect(component.pageData.items).toEqual([]);
      expect(component.pageData.totalElements).toBe(0);
      expect(component.error).toBe(null);
      expect(component.loading).toBe(false);
    });

    it('error: should set error + reset emptyPage + loading=false', () => {
      const stream$ = new Subject<PageResponse<DonationAdminRowResponse>>();
      apiMock.listDonationsPage.mockReturnValue(stream$.asObservable());

      // set a non-empty page to verify reset
      component.pageData = makePage(
        [
          {
            id: '99',
            createdAt: '2026-01-01T00:00:00Z',
            amountCents: 999,
            currency: 'EUR',
            status: 'PAID',
          },
        ],
        { totalElements: 1 }
      );

      component.load();
      expect(component.loading).toBe(true);

      stream$.error(new Error('boom'));

      expect(component.loading).toBe(false);
      expect(component.error).toBe('Unable to load donations');
      expect(component.pageData.items).toEqual([]);
      expect(component.pageData.totalElements).toBe(0);
    });

    it('should unsubscribe on ngOnDestroy (takeUntil): next after destroy should not change pageData', () => {
      const stream$ = new Subject<PageResponse<DonationAdminRowResponse>>();
      apiMock.listDonationsPage.mockReturnValue(stream$.asObservable());

      component.load();

      const before = component.pageData;
      component.ngOnDestroy();

      stream$.next(
        makePage([
          {
            id: '1',
            createdAt: '2026-01-01T10:00:00Z',
            amountCents: 1000,
            currency: 'EUR',
            status: 'PAID',
          },
        ])
      );

      expect(component.pageData).toBe(before);
    });
  });

  describe('pagination', () => {
    it('nextPage() does nothing if last=true', () => {
      component.page = 0;
      component.pageData = makePage([], { last: true });

      const loadSpy = vi.spyOn(component, 'load').mockImplementation(() => {});
      component.nextPage();

      expect(component.page).toBe(0);
      expect(loadSpy).not.toHaveBeenCalled();
    });

    it('nextPage() increments page and calls load if last=false', () => {
      component.page = 0;
      component.pageData = makePage([], { last: false });

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

  describe('donations / filtered', () => {
    beforeEach(() => {
      component.pageData = makePage([
        {
          id: '1',
          createdAt: '2026-01-01T10:00:00Z',
          amountCents: 1000,
          currency: 'EUR',
          status: 'PAID',
          emailMasked: 'john***@mail.com',
        },
        {
          id: '2',
          createdAt: '2026-01-02T10:00:00Z',
          amountCents: 2000,
          currency: 'EUR',
          status: 'PENDING',
          emailMasked: 'alice***@mail.com',
        },
        {
          id: '10',
          createdAt: '2026-01-03T10:00:00Z',
          amountCents: 3000,
          currency: 'EUR',
          status: 'PAID',
          emailMasked: 'bob***@mail.com',
        },
      ]);
    });

    it('donations returns items (or [])', () => {
      expect(component.donations.map((d) => d.id)).toEqual(['1', '2', '10']);
    });

    it('filtered filters by q (id or emailMasked) case-insensitive', () => {
      component.status = 'ALL';

      component.q = '10';
      expect(component.filtered.map((d) => d.id)).toEqual(['10']);

      component.q = 'ALICE';
      expect(component.filtered.map((d) => d.id)).toEqual(['2']);

      component.q = '';
      expect(component.filtered.length).toBe(3);
    });

    it('filtered filters by status if status != ALL', () => {
      component.q = '';

      component.status = 'PAID';
      expect(component.filtered.map((d) => d.id)).toEqual(['1', '10']);

      component.status = 'PENDING';
      expect(component.filtered.map((d) => d.id)).toEqual(['2']);
    });

    it('filtered combines q + status', () => {
      component.status = 'PAID';
      component.q = 'bob';
      expect(component.filtered.map((d) => d.id)).toEqual(['10']);
    });
  });
});
