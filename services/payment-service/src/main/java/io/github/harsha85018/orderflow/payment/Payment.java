package io.github.harsha85018.orderflow.payment;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "payments")
public class Payment {

    // MySQL has no UUID column type, so UUIDs are stored as CHAR(36) text.
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    // Store as plain text rather than MySQL's native ENUM type.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Payment() {}

    public Payment(UUID orderId, long amountCents, PaymentStatus status) {
        this.orderId = orderId;
        this.amountCents = amountCents;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public long getAmountCents() { return amountCents; }
    public PaymentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
