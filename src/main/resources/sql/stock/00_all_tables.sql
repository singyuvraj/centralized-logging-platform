-- Master SQL script to create all stock-related tables
-- Execute this script to set up the complete stock module database schema
-- Run this script in a single transaction for consistency
-- 
-- Prerequisites:
-- - suljhaoo schema must exist
-- - stores table must exist (from auth module)

BEGIN;

-- ============================================================================
-- STOCKS TABLE
-- ============================================================================
-- Stores inventory/stock items in the store
CREATE TABLE IF NOT EXISTS suljhaoo.stocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    quantity DECIMAL(10, 2) NOT NULL CHECK (quantity >= 0),
    min_level DECIMAL(10, 2) NOT NULL CHECK (min_level >= 0),
    unit VARCHAR(50),
    category VARCHAR(100),
    unit_price DECIMAL(10, 2) CHECK (unit_price >= 0),
    description TEXT,
    supplier_id UUID,
    supplier_name VARCHAR(255),
    store_id VARCHAR(26) NOT NULL,
    
    -- Sync fields for mobile app synchronization
    server_id VARCHAR(255),
    last_synced_at TIMESTAMP,
    is_dirty BOOLEAN NOT NULL DEFAULT false,
    
    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_stock_store FOREIGN KEY (store_id) 
        REFERENCES suljhaoo.stores(id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_stock_supplier FOREIGN KEY (supplier_id) 
        REFERENCES suljhaoo.suppliers(id) 
        ON DELETE SET NULL
);

COMMENT ON TABLE suljhaoo.stocks IS 'Stores inventory/stock items in the store with quantities, pricing, and supplier information';
COMMENT ON COLUMN suljhaoo.stocks.quantity IS 'Current quantity of stock item';
COMMENT ON COLUMN suljhaoo.stocks.min_level IS 'Minimum stock level before reordering is needed';
COMMENT ON COLUMN suljhaoo.stocks.unit_price IS 'Unit price per item/unit';
COMMENT ON COLUMN suljhaoo.stocks.supplier_id IS 'Optional reference to supplier';
COMMENT ON COLUMN suljhaoo.stocks.server_id IS 'Server ID for mobile app synchronization';
COMMENT ON COLUMN suljhaoo.stocks.is_dirty IS 'Flag indicating if record needs synchronization';

-- Indexes for stocks table
-- Store-based queries (most common)
CREATE INDEX IF NOT EXISTS idx_stock_store_id ON suljhaoo.stocks(store_id);
CREATE INDEX IF NOT EXISTS idx_stock_store_category ON suljhaoo.stocks(store_id, category) WHERE category IS NOT NULL;

-- Supplier-based queries
CREATE INDEX IF NOT EXISTS idx_stock_supplier_id ON suljhaoo.stocks(supplier_id) WHERE supplier_id IS NOT NULL;

-- Search and filtering
CREATE INDEX IF NOT EXISTS idx_stock_name ON suljhaoo.stocks(name);
CREATE INDEX IF NOT EXISTS idx_stock_category ON suljhaoo.stocks(category) WHERE category IS NOT NULL;

-- Low stock alerts (optimized for WHERE quantity <= min_level queries)
CREATE INDEX IF NOT EXISTS idx_stock_low_stock ON suljhaoo.stocks(store_id, quantity, min_level) 
    WHERE quantity <= min_level;

-- Below commands are not yet executed in the database. However, indexes are present in the
-- Sync operations
CREATE INDEX IF NOT EXISTS idx_stock_server_id ON suljhaoo.stocks(server_id) WHERE server_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_stock_is_dirty ON suljhaoo.stocks(is_dirty, store_id) WHERE is_dirty = true;

