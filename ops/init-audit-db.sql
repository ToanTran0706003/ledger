-- Tạo DB riêng cho audit-service (microservice Phase 9) khi khởi tạo Postgres container.
CREATE DATABASE ledger_audit;
GRANT ALL PRIVILEGES ON DATABASE ledger_audit TO ledger;
