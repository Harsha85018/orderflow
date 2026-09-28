package io.github.harsha85018.orderflow.order;

import java.time.Instant;
import java.util.UUID;

// The message other services receive. eventId lets consumers ignore duplicates.
public record OrderCreatedEvent(
        UUID eventId,
        String eventType,
        UUID orderId,
        String customerId,
        String productId,
        int quantity,
        long amountCents,
        Instant createdAt) {}
