package com.yassine.donationplatform.dto.response;

import com.yassine.donationplatform.domain.receipt.TaxReceiptStatus;

import java.time.Instant;
import java.util.UUID;

public class ReceiptResponse {
    private UUID id;
    private UUID donationId;
    private Long receiptNumber;
    private TaxReceiptStatus status;
    private Instant requestedAt;

    public ReceiptResponse(UUID id, UUID donationId, Long receiptNumber, TaxReceiptStatus status, Instant requestedAt) {
        this.id = id;
        this.donationId = donationId;
        this.receiptNumber = receiptNumber;
        this.status = status;
        this.requestedAt = requestedAt;
    }

    public UUID getId() { return id; }
    public UUID getDonationId() { return donationId; }
    public Long getReceiptNumber() { return receiptNumber; }
    public TaxReceiptStatus getStatus() { return status; }
    public Instant getRequestedAt() { return requestedAt; }
}
