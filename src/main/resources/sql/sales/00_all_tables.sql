-- Master SQL script to create all sales-related tables
-- Execute this script to set up the complete sales module database schema
-- Run this script in a single transaction for consistency

BEGIN;

-- Create sales table
CREATE TABLE IF NOT EXISTS suljhaoo.sales (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(26) NOT NULL,
    store_id VARCHAR(26) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    customer_name TEXT,
    note TEXT,
    sale_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 1,
    updated_by VARCHAR(20) NOT NULL DEFAULT 'web',
    CONSTRAINT fk_sale_user FOREIGN KEY (user_id) 
        REFERENCES suljhaoo.users(id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_sale_store FOREIGN KEY (store_id) 
        REFERENCES suljhaoo.stores(id) 
        ON DELETE CASCADE
);

COMMENT ON TABLE suljhaoo.sales IS 'Stores sales records (cash, upi, card, credit) for shop owners';

-- Create indexes for sales table (matching JPA entity definitions)
-- All indexes are created separately after table creation (not as constraints in CREATE TABLE)
CREATE INDEX IF NOT EXISTS idx_user_id ON suljhaoo.sales(user_id);
CREATE INDEX IF NOT EXISTS idx_store_id ON suljhaoo.sales(store_id);
CREATE INDEX IF NOT EXISTS idx_sale_date ON suljhaoo.sales(sale_date);
CREATE INDEX IF NOT EXISTS idx_user_store_date ON suljhaoo.sales(user_id, store_id, sale_date);
CREATE INDEX IF NOT EXISTS idx_store_date ON suljhaoo.sales(store_id, sale_date);

COMMIT;

-- Verification queries (optional - uncomment to verify)
-- SELECT table_name FROM information_schema.tables WHERE table_schema = 'suljhaoo' AND table_name = 'sales';
-- SELECT indexname, indexdef FROM pg_indexes WHERE schemaname = 'suljhaoo' AND tablename = 'sales' ORDER BY indexname;

