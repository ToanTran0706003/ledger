# Ledger API Gateway

Standalone Spring Cloud Gateway entrypoint for the Ledger services.

## Routes

| Public path | Downstream | Default URL | Filter |
| --- | --- | --- | --- |
| `/api/core/**` | ledger-core | `http://localhost:8080` | `StripPrefix=2` |
| `/api/audit/**` | audit-service | `http://localhost:8081` | `StripPrefix=2` |
| `/api/saga/**` | saga-orchestrator | `http://localhost:8082` | `StripPrefix=2` |

Example: `/api/core/auth/login` is forwarded to ledger-core as `/auth/login`.

## Run

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
.\gradlew.bat --no-daemon bootRun
```

The gateway listens on port `8090`.

## Environment

| Variable | Default |
| --- | --- |
| `LEDGER_CORE_URL` | `http://localhost:8080` |
| `AUDIT_URL` | `http://localhost:8081` |
| `SAGA_URL` | `http://localhost:8082` |
| `OTLP_ENDPOINT` | `http://localhost:4318/v1/traces` |
