package com.ledger.shared.eventstore;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventStoreConfig {

    /** Registry rỗng; mỗi module tự đăng ký event của mình vào đây khi khởi động. */
    @Bean
    public EventTypeRegistry eventTypeRegistry() {
        return new EventTypeRegistry();
    }
}
