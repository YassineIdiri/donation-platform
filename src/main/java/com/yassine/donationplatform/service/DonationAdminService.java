package com.yassine.donationplatform.service;

import com.yassine.donationplatform.domain.donation.Donation;
import com.yassine.donationplatform.domain.donation.DonationStatus;
import com.yassine.donationplatform.dto.response.DonationAdminRowResponse;
import com.yassine.donationplatform.repository.DonationRepository;
import com.yassine.donationplatform.util.EmailMasker;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
public class DonationAdminService {

    private final DonationRepository repo;

    public DonationAdminService(DonationRepository repo) {
        this.repo = repo;
    }

    public Page<DonationAdminRowResponse> listPage(LocalDate from, LocalDate to, DonationStatus status, int page, int size) {

        Specification<Donation> spec = null;

        if (from != null) {
            Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
            spec = and(spec, (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), fromInstant));
        }

        if (to != null) {
            Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            spec = and(spec, (root, q, cb) -> cb.lessThan(root.get("createdAt"), toInstant));
        }

        if (status != null) {
            spec = and(spec, (root, q, cb) -> cb.equal(root.get("status"), status));
        }

        int safeSize = Math.min(Math.max(size, 1), 200); // 1..200
        int safePage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending());

        Page<Donation> donationPage = (spec == null)
                ? repo.findAll(pageable)
                : repo.findAll(spec, pageable);

        return donationPage.map(d -> new DonationAdminRowResponse(
                d.getId(),
                d.getCreatedAt(),
                d.getAmountCents(),
                d.getCurrency(),
                d.getStatus(),
                d.getProvider(),
                d.getPaymentMethod(),
                EmailMasker.mask(d.getEmail())
        ));
    }

    private static <T> Specification<T> and(Specification<T> base, Specification<T> add) {
        return base == null ? add : base.and(add);
    }
}
