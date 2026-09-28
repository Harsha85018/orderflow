package io.github.harsha85018.orderflow.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    // Simulated card limit: anything above $500 is declined.
    private static final long DECLINE_ABOVE_CENTS = 50_000;

    private final PaymentRepository payments;

    public PaymentController(PaymentRepository payments) {
        this.payments = payments;
    }

    public record ChargeRequest(
            @NotNull UUID orderId,
            @Min(1) long amountCents) {}

    @PostMapping
    public ResponseEntity<Payment> charge(@Valid @RequestBody ChargeRequest req) {
        // Idempotency: if this order was already charged, return the original
        // result instead of charging again.
        return payments.findByOrderId(req.orderId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    PaymentStatus status = req.amountCents() > DECLINE_ABOVE_CENTS
                            ? PaymentStatus.DECLINED
                            : PaymentStatus.AUTHORIZED;
                    Payment payment = payments.save(
                            new Payment(req.orderId(), req.amountCents(), status));
                    return ResponseEntity.status(HttpStatus.CREATED).body(payment);
                });
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> get(@PathVariable UUID id) {
        return payments.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
