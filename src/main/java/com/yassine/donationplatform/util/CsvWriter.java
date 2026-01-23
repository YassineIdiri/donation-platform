package com.yassine.donationplatform.util;

import com.yassine.donationplatform.dto.response.DonationAdminRowResponse;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class CsvWriter {
    private CsvWriter() {}

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

    public static byte[] donationsToCsv(List<DonationAdminRowResponse> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("id;date_utc;amount_cents;currency;status;provider;payment_method;email_masked\n");

        for (DonationAdminRowResponse r : rows) {
            sb.append(r.getId()).append(';')
                    .append(ISO.format(r.getCreatedAt())).append(';')
                    .append(r.getAmountCents()).append(';')
                    .append(escape(r.getCurrency())).append(';')
                    .append(r.getStatus()).append(';')
                    .append(r.getProvider()).append(';')
                    .append(r.getPaymentMethod()).append(';')
                    .append(escape(r.getEmailMasked()))
                    .append('\n');
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String escape(String v) {
        if (v == null) return "";
        if (v.contains(";") || v.contains("\n") || v.contains("\"")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
