package com.yassine.donationplatform.dto.response;

import com.yassine.donationplatform.dto.DonationStatus;

import java.time.Instant;
import java.util.UUID;

public class DonationResponse {
    private UUID id;
    private int amountCents;
    private String currency;
    private DonationStatus status;
    private Instant createdAt;

    public DonationResponse(UUID id, int amountCents, String currency, DonationStatus status, Instant createdAt) {
        this.id = id;
        this.amountCents = amountCents;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public int getAmountCents() { return amountCents; }
    public String getCurrency() { return currency; }
    public DonationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
