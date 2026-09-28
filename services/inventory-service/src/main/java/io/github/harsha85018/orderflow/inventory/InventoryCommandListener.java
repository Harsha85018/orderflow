package io.github.harsha85018.orderflow.inventory;

import io.github.harsha85018.orderflow.inventory.Messages.InventoryCommand;
import io.github.harsha85018.orderflow.inventory.Messages.InventoryReply;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class InventoryCommandListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryCommandListener.class);

    private final InventoryService inventory;
    private final KafkaTemplate<String, String> kafka;
    private final JsonMapper json;

    public InventoryCommandListener(InventoryService inventory,
                                    KafkaTemplate<String, String> kafka, JsonMapper json) {
        this.inventory = inventory;
        this.kafka = kafka;
        this.json = json;
    }

    @KafkaListener(topics = Messages.INVENTORY_COMMANDS)
    public void onCommand(String payload) throws Exception {
        InventoryCommand cmd = json.readValue(payload, InventoryCommand.class);

        String replyType = switch (cmd.type()) {
            case "RESERVE_STOCK" -> reserve(cmd);
            case "RELEASE_STOCK" -> {
                inventory.release(cmd.orderId());
                yield "STOCK_RELEASED";
            }
            default -> {
                log.warn("Unknown inventory command {}", cmd.type());
                yield null;
            }
        };
        if (replyType == null) {
            return;
        }

        // The DB transaction has already committed at this point. If we crash
        // before this send completes, Kafka redelivers the command, the
        // idempotent reserve/release returns the same result, and we reply then.
        InventoryReply reply = new InventoryReply(UUID.randomUUID(), replyType, cmd.orderId());
        kafka.send(Messages.INVENTORY_REPLIES, cmd.orderId().toString(), json.writeValueAsString(reply))
                .get(10, TimeUnit.SECONDS);
        log.info("{} for order {} -> {}", cmd.type(), cmd.orderId(), replyType);
    }

    private String reserve(InventoryCommand cmd) {
        try {
            var result = inventory.reserve(cmd.orderId(), cmd.productId(), cmd.quantity());
            return result.reservation().getStatus() == ReservationStatus.RESERVED
                    ? "STOCK_RESERVED"
                    : "STOCK_REJECTED";
        } catch (ResponseStatusException e) {
            // Unknown product: reject rather than retry forever.
            return "STOCK_REJECTED";
        }
    }
}
