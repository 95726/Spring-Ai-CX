package com.example.springai.controller;

import com.example.springai.entity.pg.TestVector;
import com.example.springai.service.TestVectorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PostgreSQL测试向量控制器
 *
 * 提供test_vectors表的增删查接口
 */
@RestController
@RequestMapping("/api/test-vector")
public class TestVectorController {

    @Autowired
    private TestVectorService testVectorService;

    /**
     * 查询所有向量数据
     *
     * @return 向量列表
     */
    @GetMapping("/list")
    public List<TestVector> list() {
        return testVectorService.findAll();
    }

    /**
     * 根据ID查询向量数据
     *
     * @param id 主键ID
     * @return 向量对象
     */
    @GetMapping("/get/{id}")
    public TestVector getById(@PathVariable Integer id) {
        return testVectorService.findById(id).orElse(null);
    }

    /**
     * 根据名称模糊查询向量数据
     *
     * @param name 名称关键词
     * @return 匹配的向量列表
     */
    @GetMapping("/search")
    public List<TestVector> searchByName(@RequestParam String name) {
        return testVectorService.findByNameContaining(name);
    }

    /**
     * 根据名称精确查询向量数据
     *
     * @param name 名称
     * @return 向量列表
     */
    @GetMapping("/findByName")
    public List<TestVector> findByName(@RequestParam String name) {
        return testVectorService.findByName(name);
    }

    /**
     * 保存向量数据
     *
     * @param testVector 向量对象
     * @return 保存后的向量对象
     */
    @PostMapping("/save")
    public TestVector save(@RequestBody TestVector testVector) {
        return testVectorService.save(testVector);
    }

    /**
     * 根据ID删除向量数据
     *
     * @param id 主键ID
     */
    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable Integer id) {
        testVectorService.deleteById(id);
    }
}
