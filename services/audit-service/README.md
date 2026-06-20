# Ledger Audit Service

Standalone event-driven audit microservice for Ledger. It consumes `ledger.events`, rebuilds an independent account balance read model in its own Postgres database, and exposes integrity endpoints that verify each currency total against the configured genesis seed amount.

## Run

After a Gradle wrapper is added:

```bash
./gradlew bootRun
```

Or build/run it from Docker or docker-compose using the included `Dockerfile`.

Default configuration expects:

- Postgres: `jdbc:postgresql://localhost:5432/ledger_audit`
- Kafka: `localhost:9092`
- HTTP port: `8081`

Environment overrides:

- `AUDIT_DB_URL`
- `AUDIT_DB_USER`
- `AUDIT_DB_PASSWORD`
- `KAFKA_BOOTSTRAP_SERVERS`

## Endpoints

- `GET /audit/integrity`
- `GET /audit/accounts/count`
- `GET /actuator/health`
