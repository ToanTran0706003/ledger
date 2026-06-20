package com.ledger.shared.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Tự tạo topic event khi bật Kafka (KafkaAdmin). Chỉ active khi {@code ledger.kafka.enabled=true}. */
@Configuration
@ConditionalOnProperty(name = "ledger.kafka.enabled", havingValue = "true")
public class KafkaTopicConfig {

    /**
     * 1 partition -> TỔNG thứ tự event, projection áp đúng trình tự (đủ cho demo). Production: nhiều
     * partition + key = aggregateId để giữ thứ tự theo từng aggregate mà vẫn song song hoá.
     */
    @Bean
    public NewTopic ledgerEventsTopic(@Value("${ledger.kafka.topic:ledger.events}") String topic) {
        return new NewTopic(topic, 1, (short) 1);
    }
}
