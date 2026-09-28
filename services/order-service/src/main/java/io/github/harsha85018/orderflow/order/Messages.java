package io.github.harsha85018.orderflow.order;

import java.time.Instant;
import java.util.UUID;

/** Every message shape this service sends or receives over Kafka. */
public final class Messages {

    private Messages() {}

    // Published to order-events for anyone interested (e.g. notifications).
    public record OrderEvent(
            UUID eventId, String eventType, UUID orderId, String customerId,
            String productId, int quantity, long amountCents, String reason, Instant createdAt) {}

    // Order service -> inventory service
    public record InventoryCommand(
            UUID messageId, String type, UUID orderId, String productId, int quantity) {}

    // Inventory service -> order service
    public record InventoryReply(UUID messageId, String type, UUID orderId) {}

    // Order service -> payment service
    public record PaymentCommand(UUID messageId, String type, UUID orderId, long amountCents) {}

    // Payment service -> order service
    public record PaymentReply(UUID messageId, String type, UUID orderId) {}
}
