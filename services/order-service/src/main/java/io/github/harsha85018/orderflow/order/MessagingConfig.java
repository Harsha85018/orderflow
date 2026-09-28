package io.github.harsha85018.orderflow.order;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class MessagingConfig {

    // All saga topics, created on startup if missing. 3 partitions each;
    // messages are keyed by order ID so each order's messages stay in order.
    @Bean
    public KafkaAdmin.NewTopics sagaTopics() {
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name(Topics.ORDER_EVENTS).partitions(3).replicas(1).build(),
                TopicBuilder.name(Topics.INVENTORY_COMMANDS).partitions(3).replicas(1).build(),
                TopicBuilder.name(Topics.INVENTORY_REPLIES).partitions(3).replicas(1).build(),
                TopicBuilder.name(Topics.PAYMENT_COMMANDS).partitions(3).replicas(1).build(),
                TopicBuilder.name(Topics.PAYMENT_REPLIES).partitions(3).replicas(1).build());
    }
}
