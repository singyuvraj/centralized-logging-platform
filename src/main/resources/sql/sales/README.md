# Sales Module SQL Scripts

This directory contains SQL scripts for creating the sales-related database tables.

## Execution

Execute the script to create or update the sales table:

```bash
psql -h <host> -U <username> -d <database> -f 00_all_tables.sql
```

Or using a connection string:

```bash
psql "postgresql://<username>:<password>@<host>:<port>/<database>" -f 00_all_tables.sql
```

The script creates the table with all required columns including `version` and `updated_by`.

## Scripts

### 00_all_tables.sql
Creates the sales table with all columns including:
- `version`: INTEGER NOT NULL DEFAULT 1 - Version number for optimistic locking, auto-incremented on each update
- `updated_by`: VARCHAR(20) NOT NULL DEFAULT 'web' - Indicates which system updated the record ('web' for backend service, 'mobile' for mobile app)

The script handles both new table creation and adding missing columns to existing tables, making it safe to run on existing databases.

## What the Script Creates

The `00_all_tables.sql` script creates:

1. **sales table**: Stores sales records (cash, upi, card, credit) for shop owners
   - Foreign key relationships with `users` and `stores` tables
   - Columns:
     - `sales_id`: VARCHAR(26) PRIMARY KEY
     - `user_id`: VARCHAR(26) NOT NULL
     - `store_id`: VARCHAR(26) NOT NULL
     - `amount`: DECIMAL(19, 2) NOT NULL
     - `payment_method`: VARCHAR(20) NOT NULL
     - `customer_name`: TEXT
     - `note`: TEXT
     - `sale_date`: TIMESTAMP NOT NULL
     - `created_at`: TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
     - `updated_at`: TIMESTAMP DEFAULT CURRENT_TIMESTAMP
     - `version`: INTEGER NOT NULL DEFAULT 1
     - `updated_by`: VARCHAR(20) NOT NULL DEFAULT 'web'
   - Indexes: 
     - `idx_user_id` on `user_id`
     - `idx_store_id` on `store_id`
     - `idx_sale_date` on `sale_date`
     - `idx_user_store_date` on `(user_id, store_id, sale_date)`
     - `idx_store_date` on `(store_id, sale_date)`

All tables are created within a single transaction to ensure atomicity.

## Important Notes

- The script uses `CREATE TABLE IF NOT EXISTS` to prevent errors if tables already exist
- All tables are created in the `suljhaoo` schema
- Foreign key constraints are included:
  - `fk_sale_user`: `user_id` references `users(id)` with CASCADE on delete
  - `fk_sale_store`: `store_id` references `stores(id)` with CASCADE on delete
- Indexes are created for performance optimization
- The `amount` column uses `DECIMAL(19, 2)` to match JPA entity precision and scale
- The `payment_method` column is VARCHAR(20) to store values like 'cash', 'upi', 'card', 'credit'
- The `sale_date` column stores the date when the sale actually occurred (can be in the past)
- The `version` column starts at 1 and is auto-incremented by 1 on each update (handled by application logic)
- The `updated_by` column defaults to 'web' for backend service operations and 'mobile' for mobile app operations

## Schema Validation

After running the script, ensure that:
- The `suljhaoo` schema exists
- The `users` and `stores` tables exist (sales table depends on them)
- The `sales` table is created with correct columns and constraints
- All indexes are created
- Foreign key relationships are established

The application is configured with `spring.jpa.hibernate.ddl-auto=validate` to ensure the database schema matches the entity definitions.

## Verification Queries

After execution, you can verify the schema using these queries (uncomment in the script):

```sql
-- List the sales table
SELECT table_name FROM information_schema.tables WHERE table_schema = 'suljhaoo' AND table_name = 'sales';

-- List all indexes for sales table
SELECT indexname, indexdef FROM pg_indexes WHERE schemaname = 'suljhaoo' AND tablename = 'sales' ORDER BY indexname;

-- Verify foreign key constraints
SELECT 
    tc.constraint_name, 
    tc.table_name, 
    kcu.column_name, 
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name 
FROM information_schema.table_constraints AS tc 
JOIN information_schema.key_column_usage AS kcu
  ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
  ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY' 
  AND tc.table_schema = 'suljhaoo'
  AND tc.table_name = 'sales';
```

