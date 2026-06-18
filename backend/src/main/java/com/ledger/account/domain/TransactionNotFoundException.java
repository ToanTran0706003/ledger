package com.ledger.account.domain;

/** Ném ra khi đảo một txId không tồn tại. */
public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(String txId) {
        super("Không tìm thấy giao dịch: " + txId);
    }
}
