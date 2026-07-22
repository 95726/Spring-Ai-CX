package com.example.springai.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * PostgreSQL 次数据源配置类。
 *
 * 配置 PostgreSQL 作为次数据源，使用 Spring Data JPA 进行 ORM 操作。
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "com.example.springai.repository.pg",
        entityManagerFactoryRef = "pgEntityManagerFactory",
        transactionManagerRef = "pgTransactionManager"
)
public class PgDataSourceConfig {

    /**
     * 使用 DataSourceProperties 解析 url，再创建 HikariDataSource。
     * 这样可以继续使用 spring.datasource.postgresql.url 配置，避免 Hikari 的 jdbcUrl 绑定问题。
     */
    @Bean(name = "pgDataSourceProperties")
    @ConfigurationProperties(prefix = "spring.datasource.postgresql")
    public DataSourceProperties pgDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "pgDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.postgresql.hikari")
    public HikariDataSource pgDataSource(
            @Qualifier("pgDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    private EntityManagerFactoryBuilder getEntityManagerFactoryBuilder() {
        return new EntityManagerFactoryBuilder(
                new HibernateJpaVendorAdapter(),
                new HashMap<>(),
                null
        );
    }

    @Bean(name = "pgEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean pgEntityManagerFactory(
            @Qualifier("pgDataSource") DataSource dataSource) {

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.put("hibernate.show_sql", true);
        properties.put("hibernate.format_sql", true);

        return getEntityManagerFactoryBuilder()
                .dataSource(dataSource)
                .packages("com.example.springai.entity.pg")
                .persistenceUnit("pg")
                .properties(properties)
                .build();
    }

    @Bean(name = "pgTransactionManager")
    public PlatformTransactionManager pgTransactionManager(
            @Qualifier("pgEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
