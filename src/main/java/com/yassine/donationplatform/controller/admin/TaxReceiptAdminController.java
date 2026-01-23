package com.yassine.donationplatform.controller.admin;

import com.yassine.donationplatform.dto.response.ReceiptResponse;
import com.yassine.donationplatform.service.receipt.TaxReceiptService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/receipts")
public class TaxReceiptAdminController {

    private final TaxReceiptService service;

    public TaxReceiptAdminController(TaxReceiptService service) {
        this.service = service;
    }

    @PostMapping("/{id}/resend")
    public ReceiptResponse resend(@PathVariable UUID id) {
        return service.resend(id);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        return service.downloadPdf(id);
    }
}
