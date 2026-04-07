CREATE TABLE exchange_rates
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_currency VARCHAR(10)    NOT NULL,
    to_currency   VARCHAR(10)    NOT NULL,
    rate          NUMERIC(19, 6) NOT NULL,
    created_at    TIMESTAMP      NOT NULL,
    updated_at    TIMESTAMP,
    CONSTRAINT uq_exchange_rate_pair UNIQUE (from_currency, to_currency)
);

ALTER TABLE transactions
    ADD COLUMN exchange_rate NUMERIC(19, 6);
