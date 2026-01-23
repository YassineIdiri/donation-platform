package com.yassine.donationplatform.controller.admin;

import com.yassine.donationplatform.dto.DonationStatus;
import com.yassine.donationplatform.dto.response.DonationAdminRowResponse;
import com.yassine.donationplatform.dto.response.PageResponse;
import com.yassine.donationplatform.service.donation.DonationAdminService;
import com.yassine.donationplatform.util.CsvWriter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static org.springframework.format.annotation.DateTimeFormat.ISO;

@RestController
@RequestMapping("/api/admin/donations")
public class DonationAdminController {

    private final DonationAdminService adminService;

    public DonationAdminController(DonationAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public PageResponse<DonationAdminRowResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to,
            @RequestParam(required = false) DonationStatus status,
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

    @GetMapping(value = "/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to,
            @RequestParam(required = false) DonationStatus status
    ) {
        var rows = adminService.listPage(from, to, status, 0, 200).getContent(); // petit garde-fou
        byte[] csv = CsvWriter.donationsToCsv(rows);

        String filename = "donations.csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.valueOf("text/csv"))
                .body(csv);
    }
}
