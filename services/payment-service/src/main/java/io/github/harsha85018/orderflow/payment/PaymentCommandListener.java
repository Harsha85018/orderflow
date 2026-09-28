package io.github.harsha85018.orderflow.payment;

import io.github.harsha85018.orderflow.payment.Messages.PaymentCommand;
import io.github.harsha85018.orderflow.payment.Messages.PaymentReply;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class PaymentCommandListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentCommandListener.class);

    private final PaymentService paymentService;
    private final KafkaTemplate<String, String> kafka;
    private final JsonMapper json;

    public PaymentCommandListener(PaymentService paymentService,
                                  KafkaTemplate<String, String> kafka, JsonMapper json) {
        this.paymentService = paymentService;
        this.kafka = kafka;
        this.json = json;
    }

    @KafkaListener(topics = Messages.PAYMENT_COMMANDS)
    public void onCommand(String payload) throws Exception {
        PaymentCommand cmd = json.readValue(payload, PaymentCommand.class);
        if (!"CHARGE_PAYMENT".equals(cmd.type())) {
            log.warn("Unknown payment command {}", cmd.type());
            return;
        }

        Payment payment = paymentService.charge(cmd.orderId(), cmd.amountCents()).payment();
        String replyType = payment.getStatus() == PaymentStatus.AUTHORIZED
                ? "PAYMENT_AUTHORIZED"
                : "PAYMENT_DECLINED";

        PaymentReply reply = new PaymentReply(UUID.randomUUID(), replyType, cmd.orderId());
        kafka.send(Messages.PAYMENT_REPLIES, cmd.orderId().toString(), json.writeValueAsString(reply))
                .get(10, TimeUnit.SECONDS);
        log.info("CHARGE_PAYMENT for order {} -> {}", cmd.orderId(), replyType);
    }
}
