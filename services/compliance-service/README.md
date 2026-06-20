# Compliance Service

Deterministic compliance evaluator for the withdrawal Saga.

## API

`POST /compliance/evaluate`

```json
{
  "accountId": "account-id",
  "amount": 123.45
}
```

Response:

```json
{
  "approved": true,
  "reason": "OK"
}
```

Amounts above `ledger.compliance.threshold` are rejected with reason `Vượt ngưỡng tuân thủ`.

## Configuration

```yaml
server.port: 8083
ledger.compliance.threshold: 50000000
```

## Run

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
gradle bootRun
```
