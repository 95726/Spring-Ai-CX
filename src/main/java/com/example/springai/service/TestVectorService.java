package com.example.springai.service;

import com.example.springai.entity.pg.TestVector;

import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL测试向量服务接口
 *
 * 提供test_vectors表的业务操作方法
 */
public interface TestVectorService {

    /**
     * 查询所有向量数据
     *
     * @return 向量列表
     */
    List<TestVector> findAll();

    /**
     * 根据ID查询向量数据
     *
     * @param id 主键ID
     * @return 向量对象
     */
    Optional<TestVector> findById(Integer id);

    /**
     * 根据名称模糊查询向量数据
     *
     * @param name 名称关键词
     * @return 匹配的向量列表
     */
    List<TestVector> findByNameContaining(String name);

    /**
     * 根据名称精确查询向量数据
     *
     * @param name 名称
     * @return 向量列表
     */
    List<TestVector> findByName(String name);

    /**
     * 保存向量数据
     *
     * @param testVector 向量对象
     * @return 保存后的向量对象
     */
    TestVector save(TestVector testVector);

    /**
     * 根据ID删除向量数据
     *
     * @param id 主键ID
     */
    void deleteById(Integer id);
}
