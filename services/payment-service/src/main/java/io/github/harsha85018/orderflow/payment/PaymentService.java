package io.github.harsha85018.orderflow.payment;

import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    // Simulated card limit: anything above $500 is declined.
    private static final long DECLINE_ABOVE_CENTS = 50_000;

    public record ChargeResult(Payment payment, boolean created) {}

    private final PaymentRepository payments;

    public PaymentService(PaymentRepository payments) {
        this.payments = payments;
    }

    /**
     * Idempotent: charging the same order twice returns the original payment.
     *
     * Deliberately not @Transactional. Two identical requests can both find no
     * existing payment and both try to insert. The UNIQUE(order_id) constraint
     * lets exactly one insert succeed. The loser's insert fails in its own
     * transaction, and the follow-up read runs in a fresh one, so it can see
     * the winner's committed row. Inside a single transaction, that read would
     * be impossible after the failure.
     */
    public ChargeResult charge(UUID orderId, long amountCents) {
        var existing = payments.findByOrderId(orderId);
        if (existing.isPresent()) {
            return new ChargeResult(existing.get(), false);
        }

        PaymentStatus status = amountCents > DECLINE_ABOVE_CENTS
                ? PaymentStatus.DECLINED
                : PaymentStatus.AUTHORIZED;
        try {
            // saveAndFlush sends the INSERT right away, so a duplicate fails here.
            return new ChargeResult(payments.saveAndFlush(new Payment(orderId, amountCents, status)), true);
        } catch (DataIntegrityViolationException raceLost) {
            // Another request inserted this order's payment first. Return theirs.
            Payment winner = payments.findByOrderId(orderId).orElseThrow(() -> raceLost);
            return new ChargeResult(winner, false);
        }
    }
}
