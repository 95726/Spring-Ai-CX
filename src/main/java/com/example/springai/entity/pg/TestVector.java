package com.example.springai.entity.pg;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * PostgreSQL 测试向量实体类。
 *
 * 对应 PostgreSQL 数据库表 test_vectors。
 */
@Data
@Entity
@Table(name = "test_vectors")
public class TestVector {

    /**
     * 主键 ID（自增）。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 名称。
     */
    @Column(name = "name", columnDefinition = "text")
    private String name;

    /**
     * pgvector 向量数据。
     *
     * 数据库列类型为 vector(3)，Java 侧使用 float[]，由 Hibernate Vector
     * 模块负责 JDBC 类型绑定和结果转换。
     */
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 3)
    @Column(name = "embedding", columnDefinition = "vector(3)")
    private float[] embedding;
}
