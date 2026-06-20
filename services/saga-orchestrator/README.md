# Saga Orchestrator

Orchestrates withdrawal requests that need compliance approval.

Flow:

1. Place a hold in ledger-core with the caller's forwarded `Authorization` header.
2. Ask compliance-service to evaluate the withdrawal.
3. Capture the hold when approved.
4. Release the hold when rejected.
5. Release the hold best-effort when compliance or capture fails after reservation.

## API

`POST /saga/withdrawals`

```json
{
  "accountId": "account-id",
  "amount": 123.45
}
```

The caller must provide `Authorization`. The same header is forwarded to ledger-core.

## Configuration

```yaml
server.port: 8082
ledger.saga.ledger-core-url: ${LEDGER_CORE_URL:http://localhost:8080}
ledger.saga.compliance-url: ${COMPLIANCE_URL:http://localhost:8083}
```

## Run

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
gradle bootRun
```
