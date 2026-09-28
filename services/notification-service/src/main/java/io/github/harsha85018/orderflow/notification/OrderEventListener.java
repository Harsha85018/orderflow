package io.github.harsha85018.orderflow.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final MongoTemplate mongo;
    private final JsonMapper json;

    public OrderEventListener(MongoTemplate mongo, JsonMapper json) {
        this.mongo = mongo;
        this.json = json;
    }

    @KafkaListener(topics = "order-events")
    public void onOrderEvent(String payload) {
        OrderCreatedEvent event = json.readValue(payload, OrderCreatedEvent.class);
        if (!"OrderCreated".equals(event.eventType())) {
            return;
        }

        try {
            // insert (not save): fails if this event ID was already stored.
            mongo.insert(Notification.forOrderCreated(event));
            log.info("Notification stored for order {} (event {})", event.orderId(), event.eventId());
        } catch (DuplicateKeyException e) {
            log.info("Duplicate event {} ignored", event.eventId());
        }
    }
}
