package com.yassine.donationplatform.controller.publicapi;

import com.yassine.donationplatform.dto.request.ReceiptRequest;
import com.yassine.donationplatform.dto.response.ReceiptResponse;
import com.yassine.donationplatform.service.receipt.TaxReceiptService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/receipts")
public class TaxReceiptPublicController {

    private final TaxReceiptService service;

    public TaxReceiptPublicController(TaxReceiptService service) {
        this.service = service;
    }

    @PostMapping("/request")
    public ReceiptResponse request(@Valid @RequestBody ReceiptRequest req) {
        return service.requestReceipt(req);
    }

    @GetMapping("/{id}")
    public ReceiptResponse get(@PathVariable UUID id) {
        return service.getById(id);
    }
}
