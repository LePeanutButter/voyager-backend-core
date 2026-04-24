-- Initialize database for Tourism Platform
-- This script runs automatically when PostgreSQL container starts

-- Create database (handled by POSTGRES_DB env var)
-- Create user (handled by POSTGRES_USER env var)

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE tourism_platform TO tourism_user;

-- Connect to the database to create schemas
\c tourism_platform;

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Set up permissions for tables
GRANT ALL ON SCHEMA public TO tourism_user;
