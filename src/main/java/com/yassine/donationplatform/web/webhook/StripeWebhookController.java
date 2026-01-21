package com.yassine.donationplatform.web.webhook;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.yassine.donationplatform.service.StripeWebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeWebhookService stripeWebhookService;

    @Value("${app.stripe.webhook-secret}")
    private String webhookSecret;

    public StripeWebhookController(StripeWebhookService stripeWebhookService) {
        this.stripeWebhookService = stripeWebhookService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<String> handle(@RequestBody String payload,
                                         @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        if (sigHeader == null || sigHeader.isBlank()) {
            log.warn("Stripe webhook called without signature header");
            return ResponseEntity.badRequest().body("missing signature");
        }

        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            // Log minimal (pas d'ID, pas de payload)
            log.debug("Stripe webhook received type={}", event.getType());

            stripeWebhookService.handle(event);
            return ResponseEntity.ok("ok");
        } catch (SignatureVerificationException e) {
            log.warn("Stripe webhook signature verification failed");
            return ResponseEntity.badRequest().body("invalid signature");
        } catch (Exception e) {
            log.error("Stripe webhook handler failed");
            return ResponseEntity.status(500).body("error");
        }
    }
}
