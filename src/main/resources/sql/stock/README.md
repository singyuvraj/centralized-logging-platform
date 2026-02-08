# Stock Module SQL Scripts

This directory contains SQL scripts for creating the stock-related database tables.

## Execution

Execute the single script to create all stock-related tables:

```bash
psql -h <host> -U <username> -d <database> -f 00_all_tables.sql
```

Or using a connection string:

```bash
psql "postgresql://<username>:<password>@<host>:<port>/<database>" -f 00_all_tables.sql
```

## Prerequisites

Before running this script, ensure that:

1. The `suljhaoo` schema exists
2. The `stores` table exists (from auth module)

## What the Script Creates

The `00_all_tables.sql` script creates three main tables in the following order:

### 1. `suppliers` Table
Stores supplier/vendor information with the following features:
- **Primary Key**: `id` (UUID)
- **Core Fields**: `name`, `contact`, `phone`, `email`, `brands`, `salesman`, `distributor_name`
- **Relationships**: 
  - `store_id` (FK to `stores` table, VARCHAR(26) - ULID format, CASCADE on delete)
- **Sync Fields**: `server_id`, `last_synced_at`, `is_dirty` (for mobile app synchronization)
- **Audit Fields**: `created_at`, `updated_at`
- **Indexes**:
  - `idx_supplier_store_id` - For store-based queries
  - `idx_supplier_store_name` - Composite index for store and name queries
  - `idx_supplier_name` - For name-based searches
  - `idx_supplier_phone` - For phone-based lookups
  - `idx_supplier_server_id` - For sync operations
  - `idx_supplier_is_dirty` - For finding records needing sync

### 2. `stocks` Table
Stores inventory/stock items in the store with the following features:
- **Primary Key**: `id` (UUID)
- **Core Fields**: `name`, `quantity`, `min_level`, `unit`, `category`, `unit_price`, `description`
- **Relationships**: 
  - `store_id` (FK to `stores` table, VARCHAR(26) - ULID format, CASCADE on delete)
  - `supplier_id` (FK to `suppliers` table, UUID, SET NULL on delete)
- **Sync Fields**: `server_id`, `last_synced_at`, `is_dirty` (for mobile app synchronization)
- **Audit Fields**: `created_at`, `updated_at`
- **Constraints**: 
  - `quantity >= 0`
  - `min_level >= 0`
  - `unit_price >= 0`
- **Indexes**:
  - `idx_stock_store_id` - For store-based queries
  - `idx_stock_store_category` - For filtering by store and category
  - `idx_stock_supplier_id` - For supplier-based queries
  - `idx_stock_name` - For name-based searches
  - `idx_stock_category` - For category filtering
  - `idx_stock_low_stock` - Optimized partial index for low stock alerts
  - `idx_stock_server_id` - For sync operations
  - `idx_stock_is_dirty` - For finding records needing sync

### 3. `supplier_items` Table
Stores items available from suppliers, optionally linked to stock items:
- **Primary Key**: `id` (UUID)
- **Core Fields**: `name`, `unit`, `category`, `current_stock`, `min_level`, `price`
- **Relationships**: 
  - `supplier_id` (FK to `suppliers` table, UUID, CASCADE on delete)
  - `store_id` (FK to `stores` table, VARCHAR(26) - ULID format, CASCADE on delete)
  - `stock_item_id` (FK to `stocks` table, UUID, SET NULL on delete) - **Optional link to stock**
- **Sync Fields**: `server_id`, `is_dirty`
- **Audit Fields**: `created_at`, `updated_at`
- **Constraints**: 
  - `current_stock >= 0`
  - `min_level >= 0`
  - `price >= 0`
- **Indexes**:
  - `idx_supplier_item_supplier_id` - For supplier-based queries
  - `idx_supplier_item_store_supplier` - Composite index for store and supplier queries
  - `idx_supplier_item_store_id` - For store-based queries
  - `idx_supplier_item_stock_id` - Partial index for stock item relationships
  - `idx_supplier_item_name` - For name-based searches
  - `idx_supplier_item_server_id` - For sync operations
  - `idx_supplier_item_is_dirty` - For finding records needing sync

## Important Notes

- All tables are created within a single transaction to ensure atomicity
- The script uses `CREATE TABLE IF NOT EXISTS` to prevent errors if tables already exist
- All tables are created in the `suljhaoo` schema
- Tables are created in order: `suppliers` → `stocks` → `supplier_items` (to satisfy foreign key dependencies)
- Foreign key constraints are included with appropriate delete rules:
  - **CASCADE**: When parent is deleted, child records are deleted
  - **SET NULL**: When parent is deleted, foreign key is set to NULL (for optional relationships)
- Indexes are created for performance optimization, including:
  - **Partial indexes** for filtered queries (e.g., low stock items, dirty records)
  - **Composite indexes** for multi-column queries
  - **Single-column indexes** for common search patterns
- Check constraints ensure data integrity (non-negative values)
- Sync fields support mobile app offline-first architecture
- **ID Types**: 
  - Stock, Supplier, and SupplierItem use UUID for primary keys
  - Store references use VARCHAR(26) to match the ULID format used in the stores table

## Schema Validation

After running the script, ensure that:
- The `suljhaoo` schema exists
- All tables are created with correct columns and constraints
- All indexes are created
- Foreign key relationships are established
- Check constraints are in place

The application is configured with `spring.jpa.hibernate.ddl-auto=validate` to ensure the database schema matches the entity definitions.

## Verification Queries

After execution, you can verify the schema using the queries provided at the end of the script (uncomment them):

1. **List tables**: Verify all three tables exist
2. **List indexes**: Verify all indexes are created
3. **List foreign keys**: Verify all foreign key constraints
4. **Row counts**: Check initial row counts (should be 0 for new tables)

## Performance Considerations

### Index Strategy

1. **Store-based queries** are the most common, so `store_id` indexes are prioritized
2. **Partial indexes** are used for filtered queries (e.g., `WHERE is_dirty = true`) to reduce index size
3. **Composite indexes** support multi-column WHERE clauses and sorting
4. **Low stock index** is optimized for the common query: `WHERE quantity <= min_level`

### Query Patterns Supported

- Get all stocks for a store
- Get stocks by category for a store
- Get low stock items for a store
- Search stocks by name
- Get all suppliers for a store
- Get supplier items for a supplier
- Find supplier items linked to a stock item
- Sync operations (find dirty records)

## Migration Notes

If you need to add new columns or modify existing tables:

1. Always use `ALTER TABLE` statements in a transaction
2. Add indexes for new columns if they'll be used in WHERE clauses
3. Update the entity classes to match schema changes
4. Test migration scripts on a development database first

## Troubleshooting

### Common Issues

1. **Foreign key constraint errors**: Ensure `stores` table exists first
2. **Schema not found**: Create the `suljhaoo` schema before running the script
3. **Index already exists**: The script uses `IF NOT EXISTS`, so this shouldn't occur
4. **Permission errors**: Ensure the database user has CREATE TABLE and CREATE INDEX permissions
