package io.github.harsha85018.orderflow.order;

import io.github.harsha85018.orderflow.order.Messages.InventoryReply;
import io.github.harsha85018.orderflow.order.Messages.PaymentReply;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class SagaReplyListener {

    private final OrderSaga saga;
    private final JsonMapper json;

    public SagaReplyListener(OrderSaga saga, JsonMapper json) {
        this.saga = saga;
        this.json = json;
    }

    @KafkaListener(topics = Topics.INVENTORY_REPLIES)
    public void onInventoryReply(String payload) {
        saga.onInventoryReply(json.readValue(payload, InventoryReply.class));
    }

    @KafkaListener(topics = Topics.PAYMENT_REPLIES)
    public void onPaymentReply(String payload) {
        saga.onPaymentReply(json.readValue(payload, PaymentReply.class));
    }
}
