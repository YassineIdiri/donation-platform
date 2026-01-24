package com.yassine.donationplatform.service.receipt;

import com.yassine.donationplatform.entity.donation.Donation;
import com.yassine.donationplatform.dto.DonationStatus;
import com.yassine.donationplatform.entity.receipt.TaxReceipt;
import com.yassine.donationplatform.dto.TaxReceiptStatus;
import com.yassine.donationplatform.dto.request.ReceiptRequest;
import com.yassine.donationplatform.dto.response.ReceiptResponse;
import com.yassine.donationplatform.repository.TaxReceiptRepository;
import com.yassine.donationplatform.service.donation.DonationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

@Service
public class TaxReceiptService {

    @PersistenceContext
    private EntityManager em;

    private final DonationService donationService;
    private final TaxReceiptRepository receiptRepo;
    private final TaxReceiptPdfService pdfService;
    private final TaxReceiptEmailService emailService;

    public TaxReceiptService(DonationService donationService,
                             TaxReceiptRepository receiptRepo,
                             TaxReceiptPdfService pdfService,
                             TaxReceiptEmailService emailService) {
        this.donationService = donationService;
        this.receiptRepo = receiptRepo;
        this.pdfService = pdfService;
        this.emailService = emailService;
    }

    @Transactional
    public ReceiptResponse requestReceipt(ReceiptRequest req) {
        UUID donationId = req.getDonationId();

        Donation donation = donationService.findById(donationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donation not found"));

        if (donation.getStatus() != DonationStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Donation not paid");
        }

        TaxReceipt receipt = receiptRepo.findByDonationId(donationId)
                .orElseGet(() -> TaxReceipt.builder()
                        .donationId(donationId)
                        .status(TaxReceiptStatus.REQUESTED)
                        .requestedAt(Instant.now())
                        .build());

        receipt.setStatus(TaxReceiptStatus.REQUESTED);
        receipt.setEmail(req.getEmail());
        receipt.setDonorFullName(req.getFullName());
        receipt.setDonorAddress(req.getAddress());
        if (receipt.getRequestedAt() == null) receipt.setRequestedAt(Instant.now());

        TaxReceipt saved = receiptRepo.saveAndFlush(receipt);

        em.refresh(saved);

        try {
            Path pdfPath = ensurePdf(saved, donation);
            emailService.sendReceipt(saved, pdfPath);

            saved.setStatus(TaxReceiptStatus.ISSUED);
            if (saved.getIssuedAt() == null) saved.setIssuedAt(Instant.now());
            saved.setPdfPath(pdfPath.toString());

            TaxReceipt issued = receiptRepo.save(saved);
            return toResponse(issued);

        } catch (Exception e) {
            saved.setStatus(TaxReceiptStatus.FAILED);
            receiptRepo.save(saved);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to issue receipt");
        }
    }

    @Transactional
    public ReceiptResponse resend(UUID receiptId) {
        TaxReceipt receipt = receiptRepo.findById(receiptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receipt not found"));

        Donation donation = donationService.findById(receipt.getDonationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donation not found"));

        if (donation.getStatus() != DonationStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Donation not paid");
        }

        try {
            Path pdfPath = ensurePdf(receipt, donation);
            receipt.setPdfPath(pdfPath.toString());

            emailService.sendReceipt(receipt, pdfPath);

            receipt.setStatus(TaxReceiptStatus.ISSUED);
            if (receipt.getIssuedAt() == null) receipt.setIssuedAt(Instant.now());

            receiptRepo.save(receipt);
            return toResponse(receipt);

        } catch (Exception e) {
            receipt.setStatus(TaxReceiptStatus.FAILED);
            receiptRepo.save(receipt);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to resend receipt");
        }
    }

    @Transactional
    public ResponseEntity<Resource> downloadPdf(UUID receiptId) {
        TaxReceipt receipt = receiptRepo.findById(receiptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receipt not found"));

        Donation donation = donationService.findById(receipt.getDonationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donation not found"));

        if (donation.getStatus() != DonationStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Donation not paid");
        }

        try {
            Path pdfPath = ensurePdf(receipt, donation);
            receipt.setPdfPath(pdfPath.toString());
            receiptRepo.save(receipt);

            byte[] bytes = Files.readAllBytes(pdfPath);
            ByteArrayResource resource = new ByteArrayResource(bytes);

            String filename = "recu-fiscal-" + safeFileReceiptNumber(receipt) + ".pdf";

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentLength(bytes.length)
                    .body(resource);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to download receipt PDF");
        }
    }

    public ReceiptResponse getById(UUID id) {
        TaxReceipt r = receiptRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receipt not found"));
        return toResponse(r);
    }

    private Path ensurePdf(TaxReceipt receipt, Donation donation) {
        String existing = receipt.getPdfPath();
        if (existing != null && !existing.isBlank()) {
            try {
                Path candidate = Path.of(existing).normalize().toAbsolutePath();
                if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                    return candidate;
                }
            } catch (Exception ignored) {
            }
        }

        return pdfService.generatePdf(receipt, donation);
    }


    private static String safeFileReceiptNumber(TaxReceipt receipt) {
        if (receipt.getReceiptNumber() != null) {
            return String.format("CERFA-%06d", receipt.getReceiptNumber());
        }
        return "DRAFT-" + receipt.getId();
    }

    private ReceiptResponse toResponse(TaxReceipt r) {
        return new ReceiptResponse(
                r.getId(),
                r.getDonationId(),
                r.getReceiptNumber(),
                r.getStatus(),
                r.getRequestedAt()
        );
    }
}
