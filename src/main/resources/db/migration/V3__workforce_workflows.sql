ALTER TABLE hotels ADD COLUMN normal_rooms_per_hour DECIMAL(8,2) NOT NULL DEFAULT 2.40;
ALTER TABLE hotels ADD COLUMN junior_rooms_per_hour DECIMAL(8,2) NOT NULL DEFAULT 1.60;
ALTER TABLE hotels ADD COLUMN president_rooms_per_hour DECIMAL(8,2) NOT NULL DEFAULT 1.20;
ALTER TABLE hotels ADD COLUMN sunday_premium_percent DECIMAL(6,2) NOT NULL DEFAULT 50.00;
ALTER TABLE hotels ADD COLUMN night_premium_percent DECIMAL(6,2) NOT NULL DEFAULT 25.00;
ALTER TABLE hotels ADD COLUMN holiday_premium_percent DECIMAL(6,2) NOT NULL DEFAULT 100.00;

ALTER TABLE shift_plans ADD COLUMN kind VARCHAR(20) NOT NULL DEFAULT 'WORK';
ALTER TABLE shift_plans ALTER COLUMN work_type_id DROP NOT NULL;

ALTER TABLE work_logs ADD COLUMN normal_rooms INTEGER NOT NULL DEFAULT 0;
ALTER TABLE work_logs ADD COLUMN junior_rooms INTEGER NOT NULL DEFAULT 0;
ALTER TABLE work_logs ADD COLUMN president_rooms INTEGER NOT NULL DEFAULT 0;
ALTER TABLE work_logs ADD COLUMN attachment_name VARCHAR(255);
ALTER TABLE work_logs ADD COLUMN attachment_data TEXT;
ALTER TABLE work_logs ADD COLUMN rejection_reason VARCHAR(500);
ALTER TABLE work_logs ADD COLUMN reviewed_by BIGINT REFERENCES app_users(id);
ALTER TABLE work_logs ADD COLUMN reviewed_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL REFERENCES app_users(id),
    title VARCHAR(150) NOT NULL,
    message VARCHAR(500) NOT NULL,
    link VARCHAR(200),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE pay_rates (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES app_users(id),
    hourly_rate DECIMAL(10,2) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_notifications_recipient ON notifications(recipient_id, is_read, created_at);
CREATE INDEX idx_pay_rates_employee_date ON pay_rates(employee_id, effective_from);

