package com.example.springai.service.impl;

import com.example.springai.entity.pg.TestVector;
import com.example.springai.repository.pg.TestVectorRepository;
import com.example.springai.service.TestVectorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL测试向量服务实现类
 *
 * 实现test_vectors表的业务操作方法
 */
@Service
@Transactional(transactionManager = "pgTransactionManager")
public class TestVectorServiceImpl implements TestVectorService {

    @Autowired
    private TestVectorRepository testVectorRepository;

    @Override
    public List<TestVector> findAll() {
        return testVectorRepository.findAll();
    }

    @Override
    public Optional<TestVector> findById(Integer id) {
        return testVectorRepository.findById(id);
    }

    @Override
    public List<TestVector> findByNameContaining(String name) {
        return testVectorRepository.findByNameContaining(name);
    }

    @Override
    public List<TestVector> findByName(String name) {
        return testVectorRepository.findByName(name);
    }

    @Override
    public TestVector save(TestVector testVector) {
        return testVectorRepository.save(testVector);
    }

    @Override
    public void deleteById(Integer id) {
        testVectorRepository.deleteById(id);
    }
}
