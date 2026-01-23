package com.yassine.donationplatform.dto.response;

import com.yassine.donationplatform.dto.DonationStatus;
import com.yassine.donationplatform.dto.PaymentMethod;
import com.yassine.donationplatform.dto.PaymentProvider;

import java.time.Instant;
import java.util.UUID;

public class DonationAdminRowResponse {
    private UUID id;
    private Instant createdAt;
    private int amountCents;
    private String currency;
    private DonationStatus status;
    private PaymentProvider provider;
    private PaymentMethod paymentMethod;
    private String emailMasked;

    public DonationAdminRowResponse(UUID id, Instant createdAt, int amountCents, String currency,
                                    DonationStatus status, PaymentProvider provider,
                                    PaymentMethod paymentMethod, String emailMasked) {
        this.id = id;
        this.createdAt = createdAt;
        this.amountCents = amountCents;
        this.currency = currency;
        this.status = status;
        this.provider = provider;
        this.paymentMethod = paymentMethod;
        this.emailMasked = emailMasked;
    }

    public UUID getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public int getAmountCents() { return amountCents; }
    public String getCurrency() { return currency; }
    public DonationStatus getStatus() { return status; }
    public PaymentProvider getProvider() { return provider; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getEmailMasked() { return emailMasked; }
}
