-- Identity & Access: tài khoản đăng nhập. Đây là dữ liệu CRUD thường (không event-sourced)
-- nên dùng JPA — minh họa tương phản với event store dùng JDBC thuần (ADR-0009).
CREATE TABLE users (
    id            UUID PRIMARY KEY,
    username      VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,        -- BCrypt
    role          VARCHAR(16) NOT NULL,         -- CUSTOMER / ADMIN / AUDITOR
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
