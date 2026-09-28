ALTER TABLE outbox_events ADD COLUMN trace_id VARCHAR(32);
ALTER TABLE outbox_events ADD COLUMN span_id VARCHAR(16);
