package io.github.harsha85018.orderflow.order;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Order() {}

    public Order(String customerId, String productId, int quantity, long amountCents) {
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.amountCents = amountCents;
        this.status = OrderStatus.PENDING;
        this.createdAt = Instant.now();
    }

    // Each transition only happens from the expected state. A duplicate or
    // out-of-order reply returns false and changes nothing.

    public boolean markStockReserved() {
        if (status != OrderStatus.PENDING) return false;
        status = OrderStatus.STOCK_RESERVED;
        return true;
    }

    public boolean confirm() {
        if (status != OrderStatus.STOCK_RESERVED) return false;
        status = OrderStatus.CONFIRMED;
        return true;
    }

    public boolean cancel(String reason) {
        if (status == OrderStatus.CONFIRMED || status == OrderStatus.CANCELLED) return false;
        status = OrderStatus.CANCELLED;
        cancelReason = reason;
        return true;
    }

    public UUID getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public long getAmountCents() { return amountCents; }
    public OrderStatus getStatus() { return status; }
    public String getCancelReason() { return cancelReason; }
    public Instant getCreatedAt() { return createdAt; }
}
