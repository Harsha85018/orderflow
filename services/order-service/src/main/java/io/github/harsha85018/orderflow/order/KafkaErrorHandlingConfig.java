package io.github.harsha85018.orderflow.order;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
public class KafkaErrorHandlingConfig {

    /**
     * When a message keeps failing (for example, the database is down), retry
     * with growing waits of 1, 2, 4, 8, and 16 seconds. If it still fails, move
     * it to a dead-letter topic instead of dropping it, so it can be replayed.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafka) {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(5);
        backOff.setInitialInterval(1_000);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(16_000);
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(kafka), backOff);
    }
}
