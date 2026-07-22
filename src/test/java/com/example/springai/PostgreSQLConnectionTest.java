package com.example.springai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PostgreSQL 连接测试。
 *
 * 测试显式使用 PostgreSQL 数据源，避免被 MySQL 主数据源（@Primary）覆盖。
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgreSQLConnectionTest.TestJdbcConfiguration.class)
class PostgreSQLConnectionTest {

    @Autowired
    @Qualifier("pgDataSource")
    private DataSource dataSource;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Test
    void testDataSourceConnection() throws Exception {
        assertNotNull(dataSource, "DataSource 不应为 null");

        try (Connection conn = dataSource.getConnection()) {
            assertFalse(conn.isClosed(), "连接应处于打开状态");

            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("数据库连接成功！");
            System.out.println("  产品: " + meta.getDatabaseProductName());
            System.out.println("  版本: " + meta.getDatabaseProductVersion());
            System.out.println("  驱动: " + meta.getDriverName() + " " + meta.getDriverVersion());
        }
    }

    @Test
    void testJdbcTemplateQuery() {
        String version = jdbcTemplate.queryForObject("SELECT version()", String.class);
        assertNotNull(version, "version() 不应返回 null");
        assertTrue(version.contains("PostgreSQL"), "结果应包含 PostgreSQL 标识");
        System.out.println("version(): " + version);
    }

    @Test
    void testListTables() {
        List<Map<String, Object>> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'public' ORDER BY table_name"
        );
        System.out.println("public schema 下共 " + tables.size() + " 张表:");
        tables.forEach(row -> System.out.println("  " + row.get("table_name")));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestJdbcConfiguration {

        @Bean(name = "pgJdbcTemplate")
        JdbcTemplate pgJdbcTemplate(@Qualifier("pgDataSource") DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }
}
