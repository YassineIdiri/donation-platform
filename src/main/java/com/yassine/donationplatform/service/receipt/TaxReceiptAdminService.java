package com.yassine.donationplatform.service.receipt;

import com.yassine.donationplatform.entity.receipt.TaxReceipt;
import com.yassine.donationplatform.dto.TaxReceiptStatus;
import com.yassine.donationplatform.dto.response.ReceiptAdminRowResponse;
import com.yassine.donationplatform.repository.TaxReceiptRepository;
import com.yassine.donationplatform.util.EmailMasker;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
public class TaxReceiptAdminService {

    private final TaxReceiptRepository repo;

    public TaxReceiptAdminService(TaxReceiptRepository repo) {
        this.repo = repo;
    }

    public Page<ReceiptAdminRowResponse> listPage(LocalDate from, LocalDate to, TaxReceiptStatus status, int page, int size) {

        Specification<TaxReceipt> spec = null;

        if (from != null) {
            Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
            spec = and(spec, (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("requestedAt"), fromInstant));
        }

        if (to != null) {
            Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            spec = and(spec, (root, q, cb) -> cb.lessThan(root.get("requestedAt"), toInstant));
        }

        if (status != null) {
            spec = and(spec, (root, q, cb) -> cb.equal(root.get("status"), status));
        }

        int safeSize = Math.min(Math.max(size, 1), 200);
        int safePage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by("requestedAt").descending()
        );

        Page<TaxReceipt> receiptPage = (spec == null)
                ? repo.findAll(pageable)
                : repo.findAll(spec, pageable);

        return receiptPage.map(r -> new ReceiptAdminRowResponse(
                r.getId(),
                r.getDonationId(),
                r.getReceiptNumber(),
                r.getStatus(),
                EmailMasker.mask(r.getEmail()),
                r.getRequestedAt(),
                r.getIssuedAt()
        ));
    }

    private static <T> Specification<T> and(Specification<T> base, Specification<T> add) {
        return base == null ? add : base.and(add);
    }
}
