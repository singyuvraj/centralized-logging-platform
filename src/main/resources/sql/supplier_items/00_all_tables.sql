
-- ============================================================================
-- SUPPLIER_ITEMS TABLE
-- ============================================================================
-- Stores items available from suppliers, optionally linked to stock items
CREATE TABLE IF NOT EXISTS suljhaoo.supplier_items (
                                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_id UUID NOT NULL,
    store_id VARCHAR(26) NOT NULL,
    stock_item_id UUID, -- Optional reference to stock item
    name VARCHAR(255) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    category VARCHAR(100),
    current_stock DECIMAL(10, 2) CHECK (current_stock >= 0),
    min_level DECIMAL(10, 2) CHECK (min_level >= 0),
    price DECIMAL(10, 2) CHECK (price >= 0),

    -- Sync fields for mobile app synchronization
    server_id VARCHAR(255),
    is_dirty BOOLEAN NOT NULL DEFAULT false,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraints
    CONSTRAINT fk_supplier_item_supplier FOREIGN KEY (supplier_id)
    REFERENCES suljhaoo.suppliers(id)
    ON DELETE CASCADE,
    CONSTRAINT fk_supplier_item_store FOREIGN KEY (store_id)
    REFERENCES suljhaoo.stores(id)
    ON DELETE CASCADE,
    CONSTRAINT fk_supplier_item_stock FOREIGN KEY (stock_item_id)
    REFERENCES suljhaoo.stocks(id)
    ON DELETE SET NULL
    );

-- Below lines are not yet executed in the database

COMMENT ON TABLE suljhaoo.supplier_items IS 'Stores items available from suppliers, optionally linked to stock items for inventory tracking';
COMMENT ON COLUMN suljhaoo.supplier_items.stock_item_id IS 'Optional reference to stock item for inventory synchronization';
COMMENT ON COLUMN suljhaoo.supplier_items.current_stock IS 'Current stock level at supplier';
COMMENT ON COLUMN suljhaoo.supplier_items.min_level IS 'Minimum level before reordering';
COMMENT ON COLUMN suljhaoo.supplier_items.server_id IS 'Server ID for mobile app synchronization';
COMMENT ON COLUMN suljhaoo.supplier_items.is_dirty IS 'Flag indicating if record needs synchronization';

-- Indexes for supplier_items table
-- Supplier-based queries (most common)
CREATE INDEX IF NOT EXISTS idx_supplier_item_supplier_id ON suljhaoo.supplier_items(supplier_id);
CREATE INDEX IF NOT EXISTS idx_supplier_item_store_supplier ON suljhaoo.supplier_items(store_id, supplier_id);

-- Store-based queries
CREATE INDEX IF NOT EXISTS idx_supplier_item_store_id ON suljhaoo.supplier_items(store_id);

-- Stock item relationship (for finding supplier items linked to a stock)
CREATE INDEX IF NOT EXISTS idx_supplier_item_stock_id ON suljhaoo.supplier_items(stock_item_id)
    WHERE stock_item_id IS NOT NULL;

-- Search and filtering
CREATE INDEX IF NOT EXISTS idx_supplier_item_name ON suljhaoo.supplier_items(name);

-- Sync operations
CREATE INDEX IF NOT EXISTS idx_supplier_item_server_id ON suljhaoo.supplier_items(server_id)
    WHERE server_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_supplier_item_is_dirty ON suljhaoo.supplier_items(is_dirty, store_id)
    WHERE is_dirty = true;

COMMIT;