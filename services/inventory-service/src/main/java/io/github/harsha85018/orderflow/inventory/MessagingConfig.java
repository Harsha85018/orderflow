package io.github.harsha85018.orderflow.inventory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class MessagingConfig {

    @Bean
    public KafkaAdmin.NewTopics inventoryTopics() {
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name(Messages.INVENTORY_COMMANDS).partitions(3).replicas(1).build(),
                TopicBuilder.name(Messages.INVENTORY_REPLIES).partitions(3).replicas(1).build());
    }
}
