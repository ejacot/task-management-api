ALTER TABLE work_logs ADD COLUMN shift_plan_id BIGINT REFERENCES shift_plans(id);
CREATE UNIQUE INDEX idx_work_logs_shift_plan ON work_logs(shift_plan_id);
