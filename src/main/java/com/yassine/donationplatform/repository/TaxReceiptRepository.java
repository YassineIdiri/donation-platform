package com.yassine.donationplatform.repository;

import com.yassine.donationplatform.domain.receipt.TaxReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface TaxReceiptRepository extends JpaRepository<TaxReceipt, UUID>, JpaSpecificationExecutor<TaxReceipt> {
    Optional<TaxReceipt> findByDonationId(UUID donationId);
    boolean existsByDonationId(UUID donationId);
}
