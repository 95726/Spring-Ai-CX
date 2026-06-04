package com.example.springai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
 * 通过 Spring Boot 自动配置注入 DataSource，使用 test profile 连接 PostgreSQL。
 */
@SpringBootTest
@ActiveProfiles("test")
class PostgreSQLConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 测试数据源是否成功注入、连接是否可用
     */
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

    /**
     * 测试通过 JdbcTemplate 执行 SQL 查询
     */
    @Test
    void testJdbcTemplateQuery() {
        // 查询 PostgreSQL 版本
        String version = jdbcTemplate.queryForObject("SELECT version()", String.class);
        assertNotNull(version, "version() 不应返回 null");
        assertTrue(version.contains("PostgreSQL"), "结果应包含 PostgreSQL 标识");
        System.out.println("version(): " + version);
    }

    /**
     * 测试列出 public schema 下的用户表
     */
    @Test
    void testListTables() {
        List<Map<String, Object>> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name"
        );
        System.out.println("public schema 下共 " + tables.size() + " 张表:");
        tables.forEach(row -> System.out.println("  " + row.get("table_name")));
    }
}
