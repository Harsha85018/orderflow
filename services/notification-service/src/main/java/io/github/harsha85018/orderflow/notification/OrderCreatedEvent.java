package io.github.harsha85018.orderflow.notification;

import java.time.Instant;
import java.util.UUID;

// This service's own copy of the event shape. Services share the message
// format, not code, so each one can be deployed independently.
public record OrderCreatedEvent(
        UUID eventId,
        String eventType,
        UUID orderId,
        String customerId,
        String productId,
        int quantity,
        long amountCents,
        Instant createdAt) {}
