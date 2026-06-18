package com.ledger.account;

import com.ledger.account.command.VaultSeedService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Seed SYSTEM_VAULT một lần khi ứng dụng khởi động (idempotent). */
@Component
public class VaultSeedRunner implements ApplicationRunner {

    private final VaultSeedService vaultSeedService;

    public VaultSeedRunner(VaultSeedService vaultSeedService) {
        this.vaultSeedService = vaultSeedService;
    }

    @Override
    public void run(ApplicationArguments args) {
        vaultSeedService.seedIfAbsent();
    }
}
