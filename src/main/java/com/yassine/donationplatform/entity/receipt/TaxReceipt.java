package com.yassine.donationplatform.entity.receipt;

import com.yassine.donationplatform.dto.TaxReceiptStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tax_receipt")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxReceipt {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "donation_id", nullable = false, updatable = false)
    private UUID donationId;

    @Column(name = "receipt_number", nullable = false, insertable = false, updatable = false)
    private Long receiptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaxReceiptStatus status;

    @Column(name = "donor_full_name", nullable = false, length = 180)
    private String donorFullName;

    @Column(name = "donor_address", nullable = false, columnDefinition = "text")
    private String donorAddress;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "pdf_path", columnDefinition = "text")
    private String pdfPath;

    @PrePersist
    void onCreate() {
        if (requestedAt == null) requestedAt = Instant.now();
    }
}
