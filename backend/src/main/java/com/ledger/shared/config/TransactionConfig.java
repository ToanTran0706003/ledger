package com.ledger.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * TransactionTemplate cho transaction lập trình (programmatic). Dùng bởi cơ chế retry
 * và outbox relay để mỗi lần thử là một transaction riêng — tránh bẫy self-invocation
 * của @Transactional theo annotation.
 */
@Configuration
public class TransactionConfig {

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
