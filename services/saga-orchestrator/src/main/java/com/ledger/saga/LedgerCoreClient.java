package com.ledger.saga;

import java.math.BigDecimal;

public interface LedgerCoreClient {

    HoldResult placeHold(String accountId, BigDecimal amount, String authorization);

    CaptureResult captureHold(String accountId, String holdId, String authorization);

    void releaseHold(String accountId, String holdId, String authorization);
}
