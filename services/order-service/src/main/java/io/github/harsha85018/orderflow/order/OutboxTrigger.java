package io.github.harsha85018.orderflow.order;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes outbox rows as soon as the transaction that wrote them commits,
 * instead of waiting up to 500 ms for the next scheduled poll.
 */
@Component
public class OutboxTrigger {

    private final OutboxPublisher publisher;

    public OutboxTrigger(OutboxPublisher publisher) {
        this.publisher = publisher;
    }

    // AFTER_COMMIT: the rows are visible to the publisher's own transaction.
    // @Async: the caller (e.g. the HTTP request) doesn't wait for Kafka.
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutboxWritten(OutboxWritten event) {
        publisher.publishPending();
    }
}
