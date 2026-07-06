CREATE TABLE hotels (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    city VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE app_users ADD COLUMN email VARCHAR(150);
ALTER TABLE app_users ADD COLUMN phone VARCHAR(30);
ALTER TABLE app_users ADD COLUMN role VARCHAR(30) NOT NULL DEFAULT 'EMPLOYEE';
ALTER TABLE app_users ADD COLUMN hourly_rate DECIMAL(10,2) NOT NULL DEFAULT 0;
ALTER TABLE app_users ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE app_users ADD COLUMN hotel_id BIGINT REFERENCES hotels(id);

CREATE UNIQUE INDEX idx_users_email ON app_users(email);
CREATE UNIQUE INDEX idx_users_phone ON app_users(phone);

CREATE TABLE work_types (
    id BIGSERIAL PRIMARY KEY,
    hotel_id BIGINT NOT NULL REFERENCES hotels(id),
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    rooms_per_hour DECIMAL(8,2),
    color VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(hotel_id, code)
);

CREATE TABLE shift_plans (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES app_users(id),
    hotel_id BIGINT NOT NULL REFERENCES hotels(id),
    work_type_id BIGINT NOT NULL REFERENCES work_types(id),
    work_date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    status VARCHAR(20) NOT NULL,
    notes VARCHAR(500),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE work_logs (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES app_users(id),
    hotel_id BIGINT NOT NULL REFERENCES hotels(id),
    work_type_id BIGINT NOT NULL REFERENCES work_types(id),
    work_date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    break_minutes INTEGER NOT NULL DEFAULT 0,
    room_type VARCHAR(30),
    quantity INTEGER,
    calculated_hours DECIMAL(8,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_plans_employee_date ON shift_plans(employee_id, work_date);
CREATE INDEX idx_logs_employee_date ON work_logs(employee_id, work_date);

