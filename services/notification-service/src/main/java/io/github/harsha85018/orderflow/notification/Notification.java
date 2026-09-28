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
    private String eventType;
    private String channel;
    private String subject;
    private String body;
    private Instant createdAt;

    protected Notification() {}

    /** Builds the notification for an order event, or null if we don't notify on it. */
    public static Notification from(OrderEvent event) {
        String amount = String.format("$%.2f", event.amountCents() / 100.0);
        String subject;
        String body;
        switch (event.eventType()) {
            case "OrderCreated" -> {
                subject = "We received your order";
                body = String.format("Order %s for %d x %s (%s) is being processed.",
                        event.orderId(), event.quantity(), event.productId(), amount);
            }
            case "OrderConfirmed" -> {
                subject = "Your order is confirmed";
                body = String.format("Order %s is confirmed and your card was charged %s.",
                        event.orderId(), amount);
            }
            case "OrderCancelled" -> {
                subject = "Your order was cancelled";
                body = String.format("Order %s was cancelled (%s). You have not been charged.",
                        event.orderId(), event.reason());
            }
            default -> {
                return null;
            }
        }

        Notification n = new Notification();
        n.id = event.eventId().toString();
        n.orderId = event.orderId().toString();
        n.customerId = event.customerId();
        n.eventType = event.eventType();
        n.channel = "EMAIL";
        n.subject = subject;
        n.body = body;
        n.createdAt = Instant.now();
        return n;
    }

    public String getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getEventType() { return eventType; }
    public String getChannel() { return channel; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
}
