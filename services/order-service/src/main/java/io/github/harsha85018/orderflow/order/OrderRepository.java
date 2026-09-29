package io.github.harsha85018.orderflow.order;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    // Orders still mid-saga after 2 minutes: a message was lost or dead-lettered.
    @Query(value = """
            SELECT count(*) FROM orders
            WHERE status IN ('PENDING', 'STOCK_RESERVED')
              AND created_at < now() - interval '2 minutes'
            """, nativeQuery = true)
    long countStuck();
}
