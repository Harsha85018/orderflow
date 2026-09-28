package io.github.harsha85018.orderflow.order;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfig {

    // The outbox poller runs every 500 ms. Without this, each empty tick would
    // show up in Jaeger as its own trace. The publisher creates a span per
    // message instead, attached to the right order's trace.
    @Bean
    public ObservationPredicate skipScheduledTaskSpans() {
        return (name, context) -> !"tasks.scheduled.execution".equals(name);
    }
}
