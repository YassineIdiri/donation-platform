package com.yassine.donationplatform.dto.response;

import com.yassine.donationplatform.dto.TaxReceiptStatus;

import java.time.Instant;
import java.util.UUID;

public class ReceiptAdminRowResponse {

    private UUID id;
    private UUID donationId;
    private Long receiptNumber;
    private TaxReceiptStatus status;
    private String emailMasked;
    private Instant requestedAt;
    private Instant issuedAt;

    public ReceiptAdminRowResponse(UUID id, UUID donationId, Long receiptNumber, TaxReceiptStatus status,
                                   String emailMasked, Instant requestedAt, Instant issuedAt) {
        this.id = id;
        this.donationId = donationId;
        this.receiptNumber = receiptNumber;
        this.status = status;
        this.emailMasked = emailMasked;
        this.requestedAt = requestedAt;
        this.issuedAt = issuedAt;
    }

    public UUID getId() { return id; }
    public UUID getDonationId() { return donationId; }
    public Long getReceiptNumber() { return receiptNumber; }
    public TaxReceiptStatus getStatus() { return status; }
    public String getEmailMasked() { return emailMasked; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getIssuedAt() { return issuedAt; }
}
