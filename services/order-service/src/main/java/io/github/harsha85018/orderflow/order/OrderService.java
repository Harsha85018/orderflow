package io.github.harsha85018.orderflow.order;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final OutboxRepository outbox;
    private final JsonMapper json;

    public OrderService(OrderRepository orders, OutboxRepository outbox, JsonMapper json) {
        this.orders = orders;
        this.outbox = outbox;
        this.json = json;
    }

    // One transaction: the order and its outbox event are saved together or not at all.
    @Transactional
    public Order create(String customerId, String productId, int quantity, long amountCents) {
        Order order = orders.save(new Order(customerId, productId, quantity, amountCents));

        UUID eventId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(
                eventId, "OrderCreated", order.getId(), customerId, productId,
                quantity, amountCents, order.getCreatedAt());

        outbox.save(new OutboxEvent(eventId, order.getId(), "OrderCreated",
                json.writeValueAsString(event)));
        return order;
    }
}
