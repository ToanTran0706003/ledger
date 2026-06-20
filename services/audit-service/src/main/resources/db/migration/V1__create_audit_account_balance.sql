CREATE TABLE audit_account_balance (
    account_id VARCHAR PRIMARY KEY,
    currency VARCHAR NOT NULL,
    balance NUMERIC(20,2) NOT NULL DEFAULT 0
);

CREATE TABLE audit_processed_money_posted (
    account_id VARCHAR NOT NULL,
    tx_id VARCHAR NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (account_id, tx_id)
);
