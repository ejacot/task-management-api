CREATE TABLE employee_requests (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES app_users(id),
    hotel_id BIGINT NOT NULL REFERENCES hotels(id),
    type VARCHAR(30) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    message VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    manager_response VARCHAR(500),
    reviewed_by BIGINT REFERENCES app_users(id),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_employee_requests_employee ON employee_requests(employee_id, created_at);
CREATE INDEX idx_employee_requests_hotel_status ON employee_requests(hotel_id, status);
