CREATE TABLE IF NOT EXISTS stock_balance (
    id              BIGSERIAL PRIMARY KEY,
    account_id      VARCHAR(100) NOT NULL,
    sku             VARCHAR(100) NOT NULL,
    quantity        INTEGER NOT NULL DEFAULT 0,
    last_updated_at TIMESTAMPTZ NOT NULL,
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_stock_balance UNIQUE (account_id, sku)
    );

CREATE TABLE IF NOT EXISTS stock_history (
    id                BIGSERIAL PRIMARY KEY,
    event_id          VARCHAR(100) NOT NULL,
    account_id        VARCHAR(100) NOT NULL,
    sku               VARCHAR(100) NOT NULL,
    event_type        VARCHAR(50)  NOT NULL,
    quantity_before   INTEGER NOT NULL,
    quantity_after    INTEGER NOT NULL,
    delta             INTEGER NOT NULL,
    marketplace       VARCHAR(100),
    external_order_id VARCHAR(100),
    reason            VARCHAR(255),
    occurred_at       TIMESTAMPTZ NOT NULL,
    processed_at      TIMESTAMPTZ NOT NULL
    );

CREATE INDEX idx_stock_history_account_sku
    ON stock_history (account_id, sku, occurred_at);

CREATE TABLE IF NOT EXISTS processed_events (
    event_id          VARCHAR(100) PRIMARY KEY,
    type              VARCHAR(50)  NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    account_id        VARCHAR(100) NOT NULL,
    sku               VARCHAR(100) NOT NULL,
    marketplace       VARCHAR(100),
    external_order_id VARCHAR(100),
    quantity          INTEGER,
    occurred_at       TIMESTAMPTZ,
    processed_at      TIMESTAMPTZ NOT NULL,
    notes             TEXT
    );

CREATE INDEX idx_processed_events_status
    ON processed_events (status);

CREATE INDEX idx_processed_events_biz_key
    ON processed_events (type, marketplace, account_id, external_order_id, sku);