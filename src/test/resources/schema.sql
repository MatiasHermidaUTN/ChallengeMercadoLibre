CREATE TABLE incidents (
    id BIGINT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL,
    resolution_code VARCHAR(20),
    resolved_at TIMESTAMP
);
