CREATE TABLE payments (
    id            CHAR(36)     NOT NULL PRIMARY KEY,
    order_id      CHAR(36)     NOT NULL,
    amount_cents  BIGINT       NOT NULL,
    status        VARCHAR(32)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    CONSTRAINT uq_payments_order UNIQUE (order_id)
);
