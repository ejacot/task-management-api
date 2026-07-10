ALTER TABLE app_users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS email_verification_code VARCHAR(20);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS email_verification_expires_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_app_users_email_verification_code ON app_users(email_verification_code);
CREATE INDEX IF NOT EXISTS idx_app_users_password_reset_code ON app_users(password_reset_code);
