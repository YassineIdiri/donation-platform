package com.yassine.donationplatform.repository;

import com.yassine.donationplatform.domain.donation.Donation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface DonationRepository extends JpaRepository<Donation, UUID>, JpaSpecificationExecutor<Donation> {
    Optional<Donation> findByStripeCheckoutSessionId(String sessionId);
}
