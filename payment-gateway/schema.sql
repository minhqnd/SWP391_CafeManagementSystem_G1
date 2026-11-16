-- Schema for Generic Payment Gateway (Cloudflare D1 / SQLite)

CREATE TABLE IF NOT EXISTS orders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    app_id TEXT DEFAULT 'default',             -- Identifier for application / project
    order_code TEXT NOT NULL,                  -- External order code (e.g., AUR-20261026-001)
    transaction_code TEXT,                     -- Internal transaction reference
    amount REAL NOT NULL,                      -- Expected amount (VND)
    status TEXT DEFAULT 'PENDING',             -- PENDING, SUCCESS, FAILED
    payment_method TEXT DEFAULT 'VIETQR',      -- VIETQR, BANK_TRANSFER, etc.
    gateway_reference TEXT,                    -- Gateway / bank reference code
    signature TEXT,                            -- Signed canonical payload once SUCCESS
    raw_webhook TEXT,                          -- Last webhook JSON received
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    paid_at DATETIME,
    UNIQUE(app_id, order_code)
);

CREATE TABLE IF NOT EXISTS webhook_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sepay_id INTEGER UNIQUE,                   -- SePay transaction ID for idempotency
    gateway TEXT,                              -- Bank name (e.g. Vietcombank, MBBank)
    account_number TEXT,
    transfer_type TEXT,                        -- 'in' or 'out'
    transfer_amount REAL,
    code TEXT,                                 -- Detected payment code
    content TEXT,                              -- Full transfer description
    reference_code TEXT,
    transaction_date TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_orders_lookup ON orders (app_id, order_code);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders (status);

-- Append-only bank ledger. Reapply this canonical schema to create the new table
-- on an existing D1 database; no existing rows are modified or reconstructed.
CREATE TABLE IF NOT EXISTS bank_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sepay_id INTEGER NOT NULL UNIQUE,
    order_id INTEGER REFERENCES orders(id),
    gateway TEXT NOT NULL,
    account_number TEXT NOT NULL,
    transfer_type TEXT NOT NULL,
    received_amount INTEGER NOT NULL CHECK(received_amount > 0),
    expected_amount REAL,
    allocated_amount REAL NOT NULL DEFAULT 0,
    excess_amount REAL NOT NULL DEFAULT 0,
    reconciliation_status TEXT NOT NULL CHECK(reconciliation_status IN
        ('MATCHED','OVERPAID','UNDERPAID','DUPLICATE_PAYMENT','UNMATCHED',
         'AMBIGUOUS','NOT_PAYABLE')),
    content TEXT NOT NULL,
    reference_code TEXT NOT NULL,
    transaction_date TEXT NOT NULL,
    raw_webhook TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_bank_transactions_order ON bank_transactions(order_id);
CREATE INDEX IF NOT EXISTS idx_bank_transactions_review ON bank_transactions(reconciliation_status, id);
