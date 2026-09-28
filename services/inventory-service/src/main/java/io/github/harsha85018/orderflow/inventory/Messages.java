package io.github.harsha85018.orderflow.inventory;

import java.util.UUID;

public final class Messages {

    private Messages() {}

    public static final String INVENTORY_COMMANDS = "inventory-commands";
    public static final String INVENTORY_REPLIES = "inventory-replies";

    public record InventoryCommand(
            UUID messageId, String type, UUID orderId, String productId, int quantity) {}

    public record InventoryReply(UUID messageId, String type, UUID orderId) {}
}
