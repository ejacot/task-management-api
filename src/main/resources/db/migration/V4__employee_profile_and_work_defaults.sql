ALTER TABLE app_users ADD COLUMN first_name VARCHAR(80);
ALTER TABLE app_users ADD COLUMN last_name VARCHAR(80);
ALTER TABLE app_users ADD COLUMN address VARCHAR(255);
ALTER TABLE app_users ADD COLUMN steuer_class INTEGER;

ALTER TABLE work_types ADD COLUMN default_start_time TIME;
ALTER TABLE work_types ADD COLUMN default_end_time TIME;
ALTER TABLE work_types ADD COLUMN default_break_minutes INTEGER NOT NULL DEFAULT 0;

UPDATE app_users SET first_name='Mariana', last_name='Jacot', address='Unterschleißheim, Germania', steuer_class=1 WHERE username='mariana';
UPDATE work_types SET default_start_time='05:00:00', default_end_time='13:30:00', default_break_minutes=30 WHERE code='PUBLIC';

