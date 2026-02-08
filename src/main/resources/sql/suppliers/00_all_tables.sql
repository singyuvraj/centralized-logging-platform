-- ============================================================================
-- SUPPLIERS TABLE
-- ============================================================================
-- Stores supplier/vendor information
CREATE TABLE IF NOT EXISTS suljhaoo.suppliers (
                                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_type VARCHAR(20) NOT NULL CHECK (supplier_type IN ('mahajan', 'distributor')),
    name VARCHAR(255) NOT NULL,
    nick_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    gstin VARCHAR(255),
    address TEXT,
    email VARCHAR(255),
    brands TEXT, -- Comma-separated brand names
    salesman VARCHAR(255),
    sales_person_phone VARCHAR(20),
    distributor_name VARCHAR(255),
    max_no_of_credit_bills INTEGER,
    max_credit_period VARCHAR(50) NOT NULL DEFAULT 'EOM',
    store_id VARCHAR(26) NOT NULL,

    -- Sync fields for mobile app synchronization
    server_id VARCHAR(255),
    last_synced_at TIMESTAMP,
    is_dirty BOOLEAN NOT NULL DEFAULT false,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraints
    CONSTRAINT fk_supplier_store FOREIGN KEY (store_id)
    REFERENCES suljhaoo.stores(id)
    ON DELETE CASCADE
    );

-- Below commands are not yet executed in the database

COMMENT ON TABLE suljhaoo.suppliers IS 'Stores supplier/vendor information and contact details';
COMMENT ON COLUMN suljhaoo.suppliers.brands IS 'Comma-separated list of brand names';
COMMENT ON COLUMN suljhaoo.suppliers.server_id IS 'Server ID for mobile app synchronization';
COMMENT ON COLUMN suljhaoo.suppliers.is_dirty IS 'Flag indicating if record needs synchronization';

-- Indexes for suppliers table
-- Store-based queries (most common)
CREATE INDEX IF NOT EXISTS idx_supplier_store_id ON suljhaoo.suppliers(store_id);
CREATE INDEX IF NOT EXISTS idx_supplier_store_name ON suljhaoo.suppliers(store_id, name);

-- Search and filtering
CREATE INDEX IF NOT EXISTS idx_supplier_name ON suljhaoo.suppliers(name);
CREATE INDEX IF NOT EXISTS idx_supplier_phone ON suljhaoo.suppliers(phone);

-- Sync operations
CREATE INDEX IF NOT EXISTS idx_supplier_server_id ON suljhaoo.suppliers(server_id) WHERE server_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_supplier_is_dirty ON suljhaoo.suppliers(is_dirty, store_id) WHERE is_dirty = true;