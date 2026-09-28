package io.github.harsha85018.orderflow.order;

import io.github.harsha85018.orderflow.order.Messages.*;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Orchestrates the order saga: reserve stock -> charge payment -> confirm,
 * with compensation (release stock) when the payment is declined.
 */
@Service
public class OrderSaga {

    private static final Logger log = LoggerFactory.getLogger(OrderSaga.class);

    private final OrderRepository orders;
    private final OutboxRepository outbox;
    private final JsonMapper json;
    private final Tracer tracer;

    public OrderSaga(OrderRepository orders, OutboxRepository outbox, JsonMapper json, Tracer tracer) {
        this.orders = orders;
        this.outbox = outbox;
        this.json = json;
        this.tracer = tracer;
    }

    @Transactional
    public Order start(String customerId, String productId, int quantity, long amountCents) {
        Order order = orders.save(new Order(customerId, productId, quantity, amountCents));
        publishOrderEvent(order, "OrderCreated");
        publish(Topics.INVENTORY_COMMANDS, order, "RESERVE_STOCK",
                new InventoryCommand(UUID.randomUUID(), "RESERVE_STOCK",
                        order.getId(), productId, quantity));
        return order;
    }

    @Transactional
    public void onInventoryReply(InventoryReply reply) {
        Order order = orders.findById(reply.orderId()).orElse(null);
        if (order == null) {
            log.warn("Inventory reply for unknown order {}", reply.orderId());
            return;
        }

        switch (reply.type()) {
            case "STOCK_RESERVED" -> {
                if (order.markStockReserved()) {
                    publish(Topics.PAYMENT_COMMANDS, order, "CHARGE_PAYMENT",
                            new PaymentCommand(UUID.randomUUID(), "CHARGE_PAYMENT",
                                    order.getId(), order.getAmountCents()));
                }
            }
            case "STOCK_REJECTED" -> {
                if (order.cancel("OUT_OF_STOCK")) {
                    publishOrderEvent(order, "OrderCancelled");
                }
            }
            case "STOCK_RELEASED" -> log.info("Stock released for order {}", order.getId());
            default -> log.warn("Unknown inventory reply type {}", reply.type());
        }
        log.info("Order {} is now {}", order.getId(), order.getStatus());
    }

    @Transactional
    public void onPaymentReply(PaymentReply reply) {
        Order order = orders.findById(reply.orderId()).orElse(null);
        if (order == null) {
            log.warn("Payment reply for unknown order {}", reply.orderId());
            return;
        }

        switch (reply.type()) {
            case "PAYMENT_AUTHORIZED" -> {
                if (order.confirm()) {
                    publishOrderEvent(order, "OrderConfirmed");
                }
            }
            case "PAYMENT_DECLINED" -> {
                if (order.cancel("PAYMENT_DECLINED")) {
                    // Compensation: undo the earlier stock reservation.
                    publish(Topics.INVENTORY_COMMANDS, order, "RELEASE_STOCK",
                            new InventoryCommand(UUID.randomUUID(), "RELEASE_STOCK",
                                    order.getId(), order.getProductId(), order.getQuantity()));
                    publishOrderEvent(order, "OrderCancelled");
                }
            }
            default -> log.warn("Unknown payment reply type {}", reply.type());
        }
        log.info("Order {} is now {}", order.getId(), order.getStatus());
    }

    private void publishOrderEvent(Order order, String eventType) {
        publish(Topics.ORDER_EVENTS, order, eventType, new OrderEvent(
                UUID.randomUUID(), eventType, order.getId(), order.getCustomerId(),
                order.getProductId(), order.getQuantity(), order.getAmountCents(),
                order.getCancelReason(), order.getCreatedAt()));
    }

    private void publish(String topic, Order order, String type, Object message) {
        OutboxEvent row = new OutboxEvent(UUID.randomUUID(), order.getId(), topic, type,
                json.writeValueAsString(message));

        // Remember which trace this message belongs to, so the publisher
        // (running later, on another thread) can continue it.
        Span current = tracer.currentSpan();
        if (current != null) {
            row.attachTrace(current.context().traceId(), current.context().spanId());
        }
        outbox.save(row);
    }
}
