# Auth Module SQL Scripts

This directory contains SQL scripts for creating the authentication-related database tables.

## Execution

Execute the single script to create all auth-related tables:

```bash
psql -h <host> -U <username> -d <database> -f 00_all_tables.sql
```

Or using a connection string:

```bash
psql "postgresql://<username>:<password>@<host>:<port>/<database>" -f 00_all_tables.sql
```

## What the Script Creates

The `00_all_tables.sql` script creates:

1. **Schema**: Creates the `suljhaoo` schema (if it doesn't exist)
2. **users table**: Stores user information and authentication details
   - Includes unique constraint on `phone_number`
   - Indexes: `idx_phone_number` (unique), `idx_default_store_id`
3. **otp table**: Stores OTP records for phone number verification
   - Index: `idx_otp_phone_number`
4. **stores table**: Stores store information linked to users
   - Foreign key relationship with `users` table
   - Indexes: `idx_user_id`, `idx_user_default`, `idx_user_deleted_active`

All tables are created within a single transaction to ensure atomicity.

## Important Notes

- The script uses `CREATE TABLE IF NOT EXISTS` to prevent errors if tables already exist
- All tables are created in the `suljhaoo` schema
- Foreign key constraints are included where applicable
- Indexes are created for performance optimization
- The `users` table has a unique constraint on `phone_number`
- The `stores` table has a foreign key relationship with `users` table (CASCADE on delete)

## Schema Validation

After running the script, ensure that:
- The `suljhaoo` schema exists
- All tables are created with correct columns and constraints
- All indexes are created
- Foreign key relationships are established

The application is configured with `spring.jpa.hibernate.ddl-auto=validate` to ensure the database schema matches the entity definitions.

## Verification Queries

After execution, you can verify the schema using these queries (uncomment in the script):

```sql
-- List all tables in the schema
SELECT table_name FROM information_schema.tables WHERE table_schema = 'suljhaoo' ORDER BY table_name;

-- List all indexes
SELECT indexname, indexdef FROM pg_indexes WHERE schemaname = 'suljhaoo' ORDER BY tablename, indexname;
```
