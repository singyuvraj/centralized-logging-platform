-- Master SQL script to create all expense-related tables
-- Execute this script to set up the complete expenses module database schema
-- Run this script in a single transaction for consistency
-- 
-- Prerequisites:
-- - suljhaoo schema must exist
-- - users table must exist (from auth module)
-- - stores table must exist (from auth module)

BEGIN;

-- ============================================================================
-- EXPENSES TABLE
-- ============================================================================
-- Stores expense records for shop owners
CREATE TABLE IF NOT EXISTS suljhaoo.expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Foreign key relationships
    user_id VARCHAR(26) NOT NULL,
    store_id VARCHAR(26) NOT NULL,
    
    -- Expense details
    category VARCHAR(255) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL CHECK (amount > 0),
    description TEXT,
    expense_date TIMESTAMP NOT NULL,
    payment_method VARCHAR(20) NOT NULL CHECK (payment_method IN ('cash', 'upi', 'card', 'bank')),
    tag VARCHAR(50) NOT NULL CHECK (tag IN ('Store', 'Staff', 'Bank', 'Govt Fees', 'Mobility')),
    bill_image_url TEXT,
    
    -- Sync fields for mobile app synchronization
    server_id VARCHAR(255),
    last_synced_at TIMESTAMP,
    is_dirty BOOLEAN NOT NULL DEFAULT false,
    
    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_expense_user FOREIGN KEY (user_id) 
        REFERENCES suljhaoo.users(id) 
        ON DELETE RESTRICT,
    CONSTRAINT fk_expense_store FOREIGN KEY (store_id) 
        REFERENCES suljhaoo.stores(id) 
        ON DELETE RESTRICT
);

COMMENT ON TABLE suljhaoo.expenses IS 'Stores expense records for shop owners with category, amount, payment method, and tags';
COMMENT ON COLUMN suljhaoo.expenses.user_id IS 'Reference to the user who created the expense';
COMMENT ON COLUMN suljhaoo.expenses.store_id IS 'Reference to the store where the expense occurred';
COMMENT ON COLUMN suljhaoo.expenses.category IS 'Expense category (e.g., Rent, Utilities, Supplies)';
COMMENT ON COLUMN suljhaoo.expenses.amount IS 'Expense amount (must be greater than 0)';
COMMENT ON COLUMN suljhaoo.expenses.expense_date IS 'Date when the expense actually occurred';
COMMENT ON COLUMN suljhaoo.expenses.payment_method IS 'Payment method: cash, upi, card, or bank';
COMMENT ON COLUMN suljhaoo.expenses.tag IS 'Expense tag: Store, Staff, Bank, Govt Fees, or Mobility';
COMMENT ON COLUMN suljhaoo.expenses.bill_image_url IS 'URL to the expense bill/receipt image';
COMMENT ON COLUMN suljhaoo.expenses.server_id IS 'Server ID for mobile app synchronization';
COMMENT ON COLUMN suljhaoo.expenses.is_dirty IS 'Flag indicating if record needs synchronization';

-- Indexes for expenses table
-- User-based queries (most common)
CREATE INDEX IF NOT EXISTS idx_expense_user_id ON suljhaoo.expenses(user_id);
CREATE INDEX IF NOT EXISTS idx_expense_user_store_date ON suljhaoo.expenses(user_id, store_id, expense_date DESC);

-- Store-based queries
CREATE INDEX IF NOT EXISTS idx_expense_store_id ON suljhaoo.expenses(store_id);
CREATE INDEX IF NOT EXISTS idx_expense_store_date ON suljhaoo.expenses(store_id, expense_date DESC);

-- Category filtering
CREATE INDEX IF NOT EXISTS idx_expense_category ON suljhaoo.expenses(category) WHERE category IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_expense_store_category ON suljhaoo.expenses(store_id, category) WHERE category IS NOT NULL;

-- Tag filtering
CREATE INDEX IF NOT EXISTS idx_expense_tag ON suljhaoo.expenses(tag) WHERE tag IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_expense_store_tag ON suljhaoo.expenses(store_id, tag) WHERE tag IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_expense_store_tag_date ON suljhaoo.expenses(store_id, tag, expense_date DESC) WHERE tag IS NOT NULL;

-- Payment method filtering
CREATE INDEX IF NOT EXISTS idx_expense_payment_method ON suljhaoo.expenses(payment_method) WHERE payment_method IS NOT NULL;

-- Date range queries
CREATE INDEX IF NOT EXISTS idx_expense_date ON suljhaoo.expenses(expense_date DESC);

-- Sync operations
CREATE INDEX IF NOT EXISTS idx_expense_server_id ON suljhaoo.expenses(server_id) WHERE server_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_expense_is_dirty ON suljhaoo.expenses(is_dirty, store_id) WHERE is_dirty = true;

COMMIT;

-- Verification queries (optional - uncomment to verify)
-- SELECT table_name FROM information_schema.tables WHERE table_schema = 'suljhaoo' AND table_name = 'expenses';
-- SELECT indexname, indexdef FROM pg_indexes WHERE schemaname = 'suljhaoo' AND tablename = 'expenses' ORDER BY indexname;
