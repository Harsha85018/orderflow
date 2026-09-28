package io.github.harsha85018.orderflow.order;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    // SKIP LOCKED: if we later run several order-service copies, each one grabs
    // different rows instead of all of them publishing the same events.
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE published_at IS NULL
            ORDER BY created_at
            LIMIT 100
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockNextBatch();

    @Query(value = "SELECT count(*) FROM outbox_events WHERE published_at IS NULL", nativeQuery = true)
    long countUnpublished();
}
