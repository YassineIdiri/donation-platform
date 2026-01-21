package com.yassine.donationplatform.web.admin;

import com.yassine.donationplatform.domain.receipt.TaxReceiptStatus;
import com.yassine.donationplatform.dto.response.PageResponse;
import com.yassine.donationplatform.dto.response.ReceiptAdminRowResponse;
import com.yassine.donationplatform.service.TaxReceiptAdminService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static org.springframework.format.annotation.DateTimeFormat.ISO;

@RestController
@RequestMapping("/api/admin/receipts")
public class TaxReceiptAdminListController {

    private final TaxReceiptAdminService adminService;

    public TaxReceiptAdminListController(TaxReceiptAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public PageResponse<ReceiptAdminRowResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to,
            @RequestParam(required = false) TaxReceiptStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var p = adminService.listPage(from, to, status, page, size);

        return new PageResponse<>(
                p.getContent(),
                p.getNumber(),
                p.getSize(),
                p.getTotalElements(),
                p.getTotalPages(),
                p.isFirst(),
                p.isLast()
        );
    }
}
