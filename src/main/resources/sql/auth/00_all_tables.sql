-- Master SQL script to create all auth-related tables
-- Execute this script to set up the complete auth module database schema
-- Run this script in a single transaction for consistency

BEGIN;

-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS suljhaoo;
COMMENT ON SCHEMA suljhaoo IS 'Main schema for suljhaoo backend service';

-- Create users table
CREATE TABLE IF NOT EXISTS suljhaoo.users (
    id VARCHAR(26) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    email VARCHAR(255),
    address TEXT,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('shopowner', 'admin', 'staff')),
    is_active BOOLEAN NOT NULL DEFAULT true,
    default_store_id VARCHAR(26),
    last_login TIMESTAMP,
    login_attempts INTEGER DEFAULT 0,
    account_locked BOOLEAN DEFAULT false,
    lock_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE suljhaoo.users IS 'Stores user information and authentication details';

-- Create indexes for users table (matching JPA entity definitions)
-- Unique index on phone_number (enforces uniqueness and matches JPA entity @Index definition)
CREATE UNIQUE INDEX IF NOT EXISTS idx_phone_number ON suljhaoo.users(phone_number);
CREATE INDEX IF NOT EXISTS idx_default_store_id ON suljhaoo.users(default_store_id);

-- Create otp table
CREATE TABLE IF NOT EXISTS suljhaoo.otp (
    id VARCHAR(26) PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL,
    otp VARCHAR(6) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE suljhaoo.otp IS 'Stores OTP records for phone number verification. TTL is handled at application level.';

-- Create index for otp table (matching JPA entity definition)
CREATE INDEX IF NOT EXISTS idx_otp_phone_number ON suljhaoo.otp(phone_number);

-- Create stores table
CREATE TABLE IF NOT EXISTS suljhaoo.stores (
    id VARCHAR(26) PRIMARY KEY,
    user_id VARCHAR(26) NOT NULL,
    store_name VARCHAR(255) NOT NULL,
    store_address TEXT,
    default_store BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_store_user FOREIGN KEY (user_id) 
        REFERENCES suljhaoo.users(id) 
        ON DELETE CASCADE
);

COMMENT ON TABLE suljhaoo.stores IS 'Stores store information linked to users';

-- Create indexes for stores table (matching JPA entity definitions)
CREATE INDEX IF NOT EXISTS idx_user_id ON suljhaoo.stores(user_id);
CREATE INDEX IF NOT EXISTS idx_user_default ON suljhaoo.stores(user_id, default_store);
CREATE INDEX IF NOT EXISTS idx_user_deleted_active ON suljhaoo.stores(user_id, is_deleted, is_active);

COMMIT;

-- Verification queries (optional - uncomment to verify)
-- SELECT table_name FROM information_schema.tables WHERE table_schema = 'suljhaoo' ORDER BY table_name;
-- SELECT indexname, indexdef FROM pg_indexes WHERE schemaname = 'suljhaoo' ORDER BY tablename, indexname;

