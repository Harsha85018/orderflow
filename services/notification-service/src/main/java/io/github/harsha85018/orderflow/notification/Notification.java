package io.github.harsha85018.orderflow.notification;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("notifications")
public class Notification {

    // The event ID is the document ID, so MongoDB itself rejects duplicates.
    @Id
    private String id;

    private String orderId;
    private String customerId;
    private String channel;
    private String subject;
    private String body;
    private Instant createdAt;

    protected Notification() {}

    public static Notification forOrderCreated(OrderCreatedEvent event) {
        Notification n = new Notification();
        n.id = event.eventId().toString();
        n.orderId = event.orderId().toString();
        n.customerId = event.customerId();
        n.channel = "EMAIL";
        n.subject = "We received your order";
        n.body = String.format("Order %s for %d x %s ($%.2f) is being processed.",
                event.orderId(), event.quantity(), event.productId(), event.amountCents() / 100.0);
        n.createdAt = Instant.now();
        return n;
    }

    public String getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getChannel() { return channel; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
}
