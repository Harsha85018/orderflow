package io.github.harsha85018.orderflow.order;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final Tracer tracer;

    public OutboxPublisher(OutboxRepository outbox, KafkaTemplate<String, String> kafka, Tracer tracer) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.tracer = tracer;
    }

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishPending() {
        for (OutboxEvent event : outbox.lockNextBatch()) {
            Span span = startSpan(event);
            try (Tracer.SpanInScope scope = tracer.withSpan(span)) {
                // Key by order ID: all messages for one order land on the same
                // partition, so consumers see them in order.
                kafka.send(event.getTopic(), event.getAggregateId().toString(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);
                event.markPublished();
            } catch (InterruptedException e) {
                span.error(e);
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                // Leave it unpublished; the next run retries it.
                span.error(e);
                log.warn("Kafka publish failed for event {}, will retry: {}", event.getId(), e.getMessage());
                return;
            } finally {
                span.end();
            }
        }
    }

    /** A span that continues the trace saved on the outbox row, if there is one. */
    private Span startSpan(OutboxEvent event) {
        Span.Builder builder = tracer.spanBuilder().name("outbox publish " + event.getEventType());
        if (event.getTraceId() != null && event.getSpanId() != null) {
            builder.setParent(tracer.traceContextBuilder()
                    .traceId(event.getTraceId())
                    .spanId(event.getSpanId())
                    .sampled(true)
                    .build());
        }
        return builder.start();
    }
}
