package com.yassine.donationplatform.service.donation;

import com.yassine.donationplatform.dto.DonationStatus;
import com.yassine.donationplatform.dto.PaymentMethod;
import com.yassine.donationplatform.dto.PaymentProvider;
import com.yassine.donationplatform.entity.donation.*;
import com.yassine.donationplatform.repository.DonationRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class DonationService {

    private final DonationRepository repo;

    public DonationService(DonationRepository repo) {
        this.repo = repo;
    }

    public Donation createDonation(int amountCents, String currency, PaymentMethod pm, String email) {
        Donation d = Donation.builder()
                .id(UUID.randomUUID())
                .amountCents(amountCents)
                .currency(currency)
                .status(DonationStatus.CREATED)
                .provider(PaymentProvider.STRIPE)
                .paymentMethod(pm)
                .email(email)
                .build();
        return repo.save(d);
    }

    public Optional<Donation> findById(UUID id) {
        return repo.findById(id);
    }

    public Optional<Donation> findBySessionId(String sessionId) {
        return repo.findByStripeCheckoutSessionId(sessionId);
    }

    public Donation save(Donation donation) {
        return repo.save(donation);
    }
}
