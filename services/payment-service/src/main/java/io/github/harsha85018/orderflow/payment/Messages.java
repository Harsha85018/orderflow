package io.github.harsha85018.orderflow.payment;

import java.util.UUID;

public final class Messages {

    private Messages() {}

    public static final String PAYMENT_COMMANDS = "payment-commands";
    public static final String PAYMENT_REPLIES = "payment-replies";

    public record PaymentCommand(UUID messageId, String type, UUID orderId, long amountCents) {}

    public record PaymentReply(UUID messageId, String type, UUID orderId) {}
}
