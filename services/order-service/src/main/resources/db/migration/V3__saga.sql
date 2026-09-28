ALTER TABLE outbox_events ADD COLUMN topic VARCHAR(128);
UPDATE outbox_events SET topic = 'order-events';
ALTER TABLE outbox_events ALTER COLUMN topic SET NOT NULL;

ALTER TABLE orders ADD COLUMN cancel_reason VARCHAR(64);
