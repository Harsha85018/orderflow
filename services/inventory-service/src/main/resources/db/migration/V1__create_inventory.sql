CREATE TABLE products (
    id                  VARCHAR(64)   PRIMARY KEY,
    name                VARCHAR(255)  NOT NULL,
    available_quantity  INTEGER       NOT NULL CHECK (available_quantity >= 0)
);

CREATE TABLE reservations (
    id          UUID         PRIMARY KEY,
    order_id    UUID         NOT NULL UNIQUE,
    product_id  VARCHAR(64)  NOT NULL REFERENCES products(id),
    quantity    INTEGER      NOT NULL CHECK (quantity > 0),
    status      VARCHAR(32)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL
);
