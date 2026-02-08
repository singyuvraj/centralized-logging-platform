-- ============================================================================
-- ORDERS TABLE
-- ============================================================================
-- Stores orders placed to suppliers
CREATE TABLE IF NOT EXISTS suljhaoo.orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(26) NOT NULL,
    store_id VARCHAR(26) NOT NULL,
    supplier_id UUID NOT NULL,
    supplier_name VARCHAR(255) NOT NULL,
    supplier_phone VARCHAR(20) NOT NULL,
    total_items INTEGER NOT NULL CHECK (total_items > 0),
    order_date TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ordered', 'received', 'cancelled', 'pending', 'closed')),
    added_to_stock BOOLEAN,

    -- Sync fields for mobile app synchronization
    server_id VARCHAR(255),
    last_synced_at TIMESTAMP,
    is_dirty BOOLEAN NOT NULL DEFAULT false,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraints
    CONSTRAINT fk_order_user FOREIGN KEY (user_id)
    REFERENCES suljhaoo.users(id)
    ON DELETE CASCADE,
    CONSTRAINT fk_order_store FOREIGN KEY (store_id)
    REFERENCES suljhaoo.stores(id)
    ON DELETE CASCADE,
    CONSTRAINT fk_order_supplier FOREIGN KEY (supplier_id)
    REFERENCES suljhaoo.suppliers(id)
    ON DELETE CASCADE
);

-- ============================================================================
-- ORDER_ITEMS TABLE
-- ============================================================================
-- Stores individual items in an order (one-to-many relationship with orders)
CREATE TABLE IF NOT EXISTS suljhaoo.order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    store_id VARCHAR(26) NOT NULL,
    item_id VARCHAR(255) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    quantity DECIMAL(10, 2) NOT NULL CHECK (quantity > 0),
    unit VARCHAR(50) NOT NULL,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraints
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id)
    REFERENCES suljhaoo.orders(id)
    ON DELETE CASCADE,
    CONSTRAINT fk_order_item_store FOREIGN KEY (store_id)
    REFERENCES suljhaoo.stores(id)
    ON DELETE CASCADE
);

-- Below commands are not yet executed in the database

COMMENT ON TABLE suljhaoo.orders IS 'Stores orders placed to suppliers';
COMMENT ON TABLE suljhaoo.order_items IS 'Stores individual items in an order';

-- Indexes for orders table
-- Only indexes on fields that are actually searched/filtered in queries

-- User-based queries: findByUser_IdOrderByOrderDateDesc, findByIdAndUser_Id, countByUser_Id
-- Composite index optimizes both filtering by user_id and sorting by order_date DESC
CREATE INDEX IF NOT EXISTS idx_order_user_date ON suljhaoo.orders(user_id, order_date DESC);

-- Supplier-based queries: findBySupplier_IdOrderByOrderDateDesc, countBySupplier_Id
-- Composite index optimizes both filtering by supplier_id and sorting by order_date DESC
CREATE INDEX IF NOT EXISTS idx_order_supplier_date ON suljhaoo.orders(supplier_id, order_date DESC);

-- Indexes for order_items table
-- Only index on order_id which is searched in findByOrder_IdOrderByCreatedAtAsc and deleteByOrder_Id
CREATE INDEX IF NOT EXISTS idx_order_item_order_id ON suljhaoo.order_items(order_id);

COMMIT;
