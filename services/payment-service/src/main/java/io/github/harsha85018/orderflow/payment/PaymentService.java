package io.github.harsha85018.orderflow.payment;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    // Simulated card limit: anything above $500 is declined.
    private static final long DECLINE_ABOVE_CENTS = 50_000;

    public record ChargeResult(Payment payment, boolean created) {}

    private final PaymentRepository payments;

    public PaymentService(PaymentRepository payments) {
        this.payments = payments;
    }

    /** Idempotent: charging the same order twice returns the original payment. */
    @Transactional
    public ChargeResult charge(UUID orderId, long amountCents) {
        var existing = payments.findByOrderId(orderId);
        if (existing.isPresent()) {
            return new ChargeResult(existing.get(), false);
        }
        PaymentStatus status = amountCents > DECLINE_ABOVE_CENTS
                ? PaymentStatus.DECLINED
                : PaymentStatus.AUTHORIZED;
        return new ChargeResult(payments.save(new Payment(orderId, amountCents, status)), true);
    }
}
