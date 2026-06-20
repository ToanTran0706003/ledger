package com.ledger.account.domain;

/** Các tài khoản hệ thống có định danh cố định. */
public final class SystemAccounts {

    /** Két VND (mặc định). Tương thích ngược: vault của VND giữ nguyên id cũ "SYSTEM_VAULT". */
    public static final String VAULT_ID = "SYSTEM_VAULT";

    /** Tiền tệ mặc định khi không nói rõ (event cũ, tài khoản cũ). */
    public static final String DEFAULT_CURRENCY = "VND";

    /** Két của một tiền tệ: VND -> "SYSTEM_VAULT" (cũ); tiền tệ khác -> "SYSTEM_VAULT:<CCY>". */
    public static String vaultFor(String currency) {
        return DEFAULT_CURRENCY.equals(currency) ? VAULT_ID : VAULT_ID + ":" + currency;
    }

    private SystemAccounts() {}
}
