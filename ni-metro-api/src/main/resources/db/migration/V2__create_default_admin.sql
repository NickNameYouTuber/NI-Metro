-- Insert default admin user with API key
-- API key: nmi-admin-2024-11-18-default-key-change-in-production
-- This should be changed in production!
INSERT INTO users (username, email, role, api_key, created_at)
VALUES (
    'admin',
    'admin@nicorp.com',
    'admin',
    'nmi-admin-2024-11-18-default-key-change-in-production',
    CURRENT_TIMESTAMP
) ON CONFLICT (username) DO NOTHING;

