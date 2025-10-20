-- Create the order_user
CREATE USER order_user WITH PASSWORD 'order_pass';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE order_db TO order_user;

-- Connect to the order_db
\c order_db;

-- Grant schema privileges
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO order_user;
GRANT ALL ON SCHEMA public TO order_user;
GRANT ALL ON ALL TABLES IN SCHEMA public TO order_user;
