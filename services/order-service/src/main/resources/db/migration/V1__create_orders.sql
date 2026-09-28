CREATE TABLE orders (
    id            UUID PRIMARY KEY,
    customer_id   VARCHAR(64)  NOT NULL,
    product_id    VARCHAR(64)  NOT NULL,
    quantity      INTEGER      NOT NULL CHECK (quantity > 0),
    amount_cents  BIGINT       NOT NULL CHECK (amount_cents > 0),
    status        VARCHAR(32)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);
