package io.github.harsha85018.orderflow.notification;

import java.time.Instant;
import java.util.UUID;

public record OrderEvent(
        UUID eventId,
        String eventType,
        UUID orderId,
        String customerId,
        String productId,
        int quantity,
        long amountCents,
        String reason,
        Instant createdAt) {}
