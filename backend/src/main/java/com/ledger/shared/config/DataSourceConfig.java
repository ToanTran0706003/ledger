package com.ledger.shared.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Tách read/write datasource (CQRS, Phase 9 — ADR-0022).
 *
 * <p>Đường GHI và mọi đọc-trong-giao-dịch (event store, projector, ownership/hạn mức/fraud) dùng
 * {@code writeDataSource} = primary (nguồn chân lý, đọc-thấy-ghi). Đường ĐỌC NẶNG cho kiểm toán/báo
 * cáo ({@code readJdbcTemplate}) tách sang read pool — prod trỏ một READ REPLICA qua
 * {@code LEDGER_READ_DATASOURCE_URL}; trên một máy dev mặc định cùng DB ghi nên không cần hạ tầng.
 * Flyway/JPA dùng primary; replica nhận schema/dữ liệu qua replication.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties writeDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource writeDataSource(DataSourceProperties writeDataSourceProperties) {
        return writeDataSourceProperties.initializeDataSourceBuilder().build();
    }

    @Bean
    @ConfigurationProperties("ledger.datasource.read")
    public DataSourceProperties readDataSourceProperties() {
        return new DataSourceProperties();
    }

    // Pool đọc NHỎ (đọc kiểm toán/báo cáo, ít đồng thời) — tránh nhân đôi kết nối tới DB.
    @Bean
    @ConfigurationProperties("ledger.datasource.read.configuration")
    public DataSource readDataSource(@Qualifier("readDataSourceProperties") DataSourceProperties readDataSourceProperties) {
        return readDataSourceProperties.initializeDataSourceBuilder().build();
    }

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(@Qualifier("writeDataSource") DataSource writeDataSource) {
        return new JdbcTemplate(writeDataSource);
    }

    @Bean
    public JdbcTemplate readJdbcTemplate(@Qualifier("readDataSource") DataSource readDataSource) {
        return new JdbcTemplate(readDataSource);
    }
}
