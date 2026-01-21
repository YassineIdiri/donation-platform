package com.yassine.donationplatform.web.publicapi;

import com.stripe.model.checkout.Session;
import com.yassine.donationplatform.domain.donation.PaymentMethod;
import com.yassine.donationplatform.dto.request.CreateCheckoutSessionRequest;
import com.yassine.donationplatform.dto.response.CheckoutSessionResponse;
import com.yassine.donationplatform.dto.response.DonationResponse;
import com.yassine.donationplatform.service.DonationService;
import com.yassine.donationplatform.service.StripeCheckoutService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class DonationPublicController {

    private final DonationService donationService;
    private final StripeCheckoutService stripeCheckoutService;

    @Value("${app.donation.min-amount-cents}")
    private int minAmountCents;

    @Value("${app.donation.currency}")
    private String currency;

    public DonationPublicController(DonationService donationService, StripeCheckoutService stripeCheckoutService) {
        this.donationService = donationService;
        this.stripeCheckoutService = stripeCheckoutService;
    }

    @PostMapping("/donations/checkout-session")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            @Valid @RequestBody CreateCheckoutSessionRequest req) throws Exception {

        int amountCents = req.getAmount() * 100;
        if (amountCents < minAmountCents) {
            return ResponseEntity.badRequest().build();
        }

        PaymentMethod pm = "PAYPAL".equalsIgnoreCase(req.getPaymentMethod())
                ? PaymentMethod.PAYPAL
                : PaymentMethod.CARD;

        var donation = donationService.createDonation(amountCents, currency, pm, req.getEmail());

        Session session = stripeCheckoutService.createCheckoutSession(donation);

        donation.setStripeCheckoutSessionId(session.getId());
        donationService.save(donation);

        return ResponseEntity.ok(new CheckoutSessionResponse(donation.getId(), session.getUrl()));
    }

    @GetMapping("/donations/{id}")
    public ResponseEntity<DonationResponse> getDonation(@PathVariable UUID id) {
        return donationService.findById(id)
                .map(d -> ResponseEntity.ok(new DonationResponse(
                        d.getId(), d.getAmountCents(), d.getCurrency(), d.getStatus(), d.getCreatedAt())))
                .orElse(ResponseEntity.notFound().build());
    }
}
