ALTER TABLE app_users ADD COLUMN IF NOT EXISTS team_name VARCHAR(120);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS deactivated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS invitation_token VARCHAR(80);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS invitation_expires_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS password_reset_code VARCHAR(20);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS password_reset_expires_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE hotels ADD COLUMN IF NOT EXISTS default_break_minutes INTEGER NOT NULL DEFAULT 30;

ALTER TABLE room_assignments ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'ASSIGNED';
ALTER TABLE room_assignments ADD COLUMN IF NOT EXISTS notes VARCHAR(500);
ALTER TABLE room_assignments ADD COLUMN IF NOT EXISTS checked_by BIGINT REFERENCES app_users(id);
ALTER TABLE room_assignments ADD COLUMN IF NOT EXISTS checked_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE room_assignments ADD COLUMN IF NOT EXISTS defect_description VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_app_users_invitation_token ON app_users(invitation_token);
CREATE INDEX IF NOT EXISTS idx_room_assignments_status_date ON room_assignments(status, work_date);
