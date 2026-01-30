-- Create users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255),
    role VARCHAR(50) NOT NULL DEFAULT 'editor',
    api_key VARCHAR(255) UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP,
    CONSTRAINT chk_role CHECK (role IN ('admin', 'editor'))
);

-- Create index on api_key for fast lookups
CREATE INDEX idx_users_api_key ON users(api_key) WHERE api_key IS NOT NULL;

-- Create maps table
CREATE TABLE maps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    country VARCHAR(255),
    version VARCHAR(50),
    author VARCHAR(255),
    icon_url VARCHAR(500),
    file_name VARCHAR(255) NOT NULL UNIQUE,
    data JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true
);

-- Create indexes on maps
CREATE INDEX idx_maps_file_name ON maps(file_name) WHERE is_active = true;
CREATE INDEX idx_maps_is_active ON maps(is_active);
CREATE INDEX idx_maps_updated_at ON maps(updated_at DESC);

-- Create notifications table
CREATE TABLE notifications (
    id VARCHAR(255) PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    trigger_type VARCHAR(50) NOT NULL,
    trigger_station_id VARCHAR(255),
    trigger_line_id VARCHAR(255),
    trigger_date_start DATE,
    trigger_date_end DATE,
    content_text TEXT,
    content_image_url VARCHAR(500),
    content_image_resource VARCHAR(255),
    content_caption TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT chk_notification_type CHECK (type IN ('normal', 'important')),
    CONSTRAINT chk_trigger_type CHECK (trigger_type IN ('once', 'date_range', 'station', 'line'))
);

-- Create indexes on notifications
CREATE INDEX idx_notifications_trigger_type ON notifications(trigger_type) WHERE is_active = true;
CREATE INDEX idx_notifications_station_id ON notifications(trigger_station_id) WHERE is_active = true AND trigger_station_id IS NOT NULL;
CREATE INDEX idx_notifications_line_id ON notifications(trigger_line_id) WHERE is_active = true AND trigger_line_id IS NOT NULL;
CREATE INDEX idx_notifications_date_range ON notifications(trigger_date_start, trigger_date_end) WHERE is_active = true AND trigger_type = 'date_range';
CREATE INDEX idx_notifications_is_active ON notifications(is_active);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at
CREATE TRIGGER update_maps_updated_at BEFORE UPDATE ON maps
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_notifications_updated_at BEFORE UPDATE ON notifications
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

