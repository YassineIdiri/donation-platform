package com.yassine.donationplatform.service.receipt;

import com.yassine.donationplatform.entity.receipt.TaxReceipt;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class TaxReceiptEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@example.com}")
    private String from;

    @Value("${app.receipt.org-name:Association}")
    private String orgName;

    public TaxReceiptEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendReceipt(TaxReceipt receipt, Path pdfPath) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(receipt.getEmail());
            helper.setSubject("Your tax receipt - " + orgName);

            String body = """
                Hello,

                Thank you for your donation. Please find your tax receipt attached.

                Best regards,
                """ + orgName;

            helper.setText(body, false);

            FileSystemResource file = new FileSystemResource(pdfPath.toFile());
            helper.addAttachment(pdfPath.getFileName().toString(), file);

            mailSender.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send receipt email", e);
        }
    }
}
