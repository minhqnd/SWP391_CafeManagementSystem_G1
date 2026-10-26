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
