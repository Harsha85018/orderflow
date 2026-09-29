package io.github.harsha85018.orderflow.order;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class SagaMetrics {

    private final MeterRegistry registry;

    public SagaMetrics(MeterRegistry registry, OutboxRepository outbox, OrderRepository orders) {
        this.registry = registry;
        // Read on every scrape: how many messages are waiting to go to Kafka.
        Gauge.builder("outbox.backlog", outbox, OutboxRepository::countUnpublished)
                .description("Outbox rows not yet published to Kafka")
                .register(registry);

        // Alert on this: sagas that stopped moving. It should always be 0.
        Gauge.builder("orders.stuck", orders, OrderRepository::countStuck)
                .description("Orders still mid-saga more than 2 minutes after creation")
                .register(registry);
    }

    public void orderStarted() {
        registry.counter("orders.placed").increment();
    }

    /** Call once, when an order reaches CONFIRMED or CANCELLED. */
    public void orderFinished(Order order) {
        String outcome = order.getStatus().name().toLowerCase(Locale.ROOT);
        String reason = order.getCancelReason() == null
                ? "none"
                : order.getCancelReason().toLowerCase(Locale.ROOT);

        registry.counter("orders.completed", "outcome", outcome, "reason", reason).increment();

        Timer.builder("saga.duration")
                .description("Time from order creation to its final state")
                .tag("outcome", outcome)
                .publishPercentileHistogram()
                .register(registry)
                .record(Duration.between(order.getCreatedAt(), Instant.now()));
    }
}
