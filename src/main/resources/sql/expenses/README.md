# Expenses Module SQL Scripts

This directory contains SQL scripts for creating the expense-related database tables.

## Execution

Execute the single script to create all expense-related tables:

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
2. The `users` table exists (from auth module)
3. The `stores` table exists (from auth module)

## What the Script Creates

The `00_all_tables.sql` script creates the following table:

### `expenses` Table

Stores expense records for shop owners with the following features:

- **Primary Key**: `id` (UUID)
- **Core Fields**: 
  - `category` - Expense category (e.g., Rent, Utilities, Supplies)
  - `amount` - Expense amount (must be greater than 0)
  - `description` - Optional description/notes
  - `expense_date` - Date when the expense actually occurred
  - `payment_method` - Payment method: 'cash', 'upi', 'card', or 'bank'
  - `tag` - Expense tag: 'Store', 'Staff', 'Bank', 'Govt Fees', or 'Mobility'
  - `bill_image_url` - URL to the expense bill/receipt image
- **Relationships**: 
  - `user_id` (FK to `users` table, VARCHAR(26) - ULID format, RESTRICT on delete)
  - `store_id` (FK to `stores` table, VARCHAR(26) - ULID format, RESTRICT on delete)
- **Sync Fields**: `server_id`, `last_synced_at`, `is_dirty` (for mobile app synchronization)
- **Audit Fields**: `created_at`, `updated_at`
- **Constraints**: 
  - `amount > 0` (CHECK constraint)
  - `payment_method IN ('cash', 'upi', 'card', 'bank')` (CHECK constraint)
  - `tag IN ('Store', 'Staff', 'Bank', 'Govt Fees', 'Mobility')` (CHECK constraint)
- **Indexes**:
  - `idx_expense_user_id` - For user-based queries
  - `idx_expense_user_store_date` - Composite index for user, store, and date queries
  - `idx_expense_store_id` - For store-based queries
  - `idx_expense_store_date` - For store and date queries
  - `idx_expense_category` - For category filtering
  - `idx_expense_store_category` - For filtering by store and category
  - `idx_expense_tag` - For tag filtering
  - `idx_expense_store_tag` - For filtering by store and tag
  - `idx_expense_store_tag_date` - For filtering by store, tag, and date
  - `idx_expense_payment_method` - For payment method filtering
  - `idx_expense_date` - For date range queries
  - `idx_expense_server_id` - For sync operations
  - `idx_expense_is_dirty` - For finding records needing sync

## Important Notes

- The table is created within a single transaction to ensure atomicity
- The script uses `CREATE TABLE IF NOT EXISTS` to prevent errors if the table already exists
- The table is created in the `suljhaoo` schema
- Foreign key constraints are included with `ON DELETE RESTRICT` to prevent accidental deletion of users/stores with associated expenses
- Indexes are created for performance optimization, including:
  - **Partial indexes** for filtered queries (e.g., dirty records, non-null categories)
  - **Composite indexes** for multi-column queries
  - **Single-column indexes** for common search patterns
- Check constraints ensure data integrity (positive amounts, valid enum values)
- Sync fields support mobile app offline-first architecture
- **ID Types**: 
  - Expense uses UUID for primary key
  - User and Store references use VARCHAR(26) to match the ULID format used in the users and stores tables

## Schema Validation

After running the script, ensure that:

- The `suljhaoo` schema exists
- The table is created with correct columns and constraints
- All indexes are created
- Foreign key relationships are established
- Check constraints are in place

The application is configured with `spring.jpa.hibernate.ddl-auto=validate` to ensure the database schema matches the entity definitions.

## Verification Queries

After execution, you can verify the schema using the queries provided at the end of the script (uncomment them):

1. **List table**: Verify the expenses table exists
2. **List indexes**: Verify all indexes are created
3. **List foreign keys**: Verify all foreign key constraints
4. **Row counts**: Check initial row count (should be 0 for new table)

## Performance Considerations

### Index Strategy

1. **Store-based queries** are the most common, so `store_id` indexes are prioritized
2. **Partial indexes** are used for filtered queries (e.g., `WHERE is_dirty = true`, `WHERE category IS NOT NULL`) to reduce index size
3. **Composite indexes** support multi-column WHERE clauses and sorting
4. **Date indexes** use DESC ordering for efficient "recent expenses" queries

### Query Patterns Supported

- Get all expenses for a user
- Get all expenses for a store
- Get expenses by date range for a store
- Get expenses by category for a store
- Get expenses by tag for a store
- Get expenses by payment method
- Get expenses by store, tag, and date
- Sync operations (find dirty records)

## Migration Notes

If you need to add new columns or modify the table:

1. Always use `ALTER TABLE` statements in a transaction
2. Add indexes for new columns if they'll be used in WHERE clauses
3. Update the entity classes to match schema changes
4. Test migration scripts on a development database first

## Troubleshooting

### Common Issues

1. **Foreign key constraint errors**: Ensure `users` and `stores` tables exist first
2. **Schema not found**: Create the `suljhaoo` schema before running the script
3. **Index already exists**: The script uses `IF NOT EXISTS`, so this shouldn't occur
4. **Permission errors**: Ensure the database user has CREATE TABLE and CREATE INDEX permissions
5. **Check constraint violations**: Ensure amount is greater than 0 and enum values are valid
