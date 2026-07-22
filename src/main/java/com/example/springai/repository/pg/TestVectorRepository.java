package com.example.springai.repository.pg;

import com.example.springai.entity.pg.TestVector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * PostgreSQL测试向量Repository
 *
 * 提供test_vectors表的查询操作
 */
@Repository
public interface TestVectorRepository extends JpaRepository<TestVector, Integer> {

    /**
     * 根据名称模糊查询
     *
     * @param name 名称关键词
     * @return 匹配的向量列表
     */
    List<TestVector> findByNameContaining(String name);

    /**
     * 根据名称精确查询
     *
     * @param name 名称
     * @return 向量列表
     */
    List<TestVector> findByName(String name);

    /**
     * 使用原生 SQL 查询向量（按名称查询）
     *
     * @param name 名称
     * @return 向量列表
     */
    @Query(value = "SELECT id, name, embedding FROM test_vectors WHERE name = :name", nativeQuery = true)
    List<TestVector> findByNameNative(@Param("name") String name);
}

