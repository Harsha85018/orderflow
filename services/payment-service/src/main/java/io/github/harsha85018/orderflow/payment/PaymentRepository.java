package io.github.harsha85018.orderflow.payment;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // Spring builds this query from the method name.
    Optional<Payment> findByOrderId(UUID orderId);
}
