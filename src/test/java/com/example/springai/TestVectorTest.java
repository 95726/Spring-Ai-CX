package com.example.springai;

import com.example.springai.entity.pg.TestVector;
import com.example.springai.repository.pg.TestVectorRepository;
import com.example.springai.service.TestVectorService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TestVector 集成测试。
 *
 * 测试对 PostgreSQL test_vectors 表的 CRUD 及查询操作。
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestVectorTest {

    @Autowired
    private TestVectorService testVectorService;

    @Autowired
    private TestVectorRepository testVectorRepository;

    /** 记录测试过程中插入的数据 ID，用于后续测试和清理。 */
    private static Integer savedId;

    // ==================== Service 层测试 ====================

    @Test
    @Order(1)
    void testSave() {
        TestVector vector = new TestVector();
        vector.setName("测试向量");
        vector.setEmbedding(new float[]{1.0f, 2.0f, 3.0f});

        TestVector saved = testVectorService.save(vector);

        assertNotNull(saved.getId(), "保存后应自动生成 ID");
        assertEquals("测试向量", saved.getName());
        assertArrayEquals(new float[]{1.0f, 2.0f, 3.0f}, saved.getEmbedding(), 0.001f);

        savedId = saved.getId();
        System.out.println("保存成功，ID = " + savedId);
    }

    @Test
    @Order(2)
    void testFindById() {
        assertNotNull(savedId, "应先生成测试数据");

        Optional<TestVector> result = testVectorService.findById(savedId);

        assertTrue(result.isPresent(), "应能根据 ID 查到数据");
        assertEquals("测试向量", result.get().getName());
        System.out.println("findById 结果: " + result.get());
    }

    @Test
    @Order(3)
    void testFindAll() {
        List<TestVector> list = testVectorService.findAll();

        assertNotNull(list);
        assertFalse(list.isEmpty(), "表中应至少有一条数据");
        System.out.println("findAll 共 " + list.size() + " 条记录");
    }

    @Test
    @Order(4)
    void testFindByName() {
        List<TestVector> list = testVectorService.findByName("测试向量");

        assertFalse(list.isEmpty(), "按精确名称应能查到数据");
        list.forEach(v -> assertEquals("测试向量", v.getName()));
        System.out.println("findByName 查到 " + list.size() + " 条");
    }

    @Test
    @Order(5)
    void testFindByNameContaining() {
        List<TestVector> list = testVectorService.findByNameContaining("向量");

        assertFalse(list.isEmpty(), "按模糊关键词应能查到数据");
        list.forEach(v -> assertTrue(v.getName().contains("向量")));
        System.out.println("findByNameContaining 查到 " + list.size() + " 条");
    }

    @Test
    @Order(6)
    void testFindByNameContaining_noMatch() {
        List<TestVector> list = testVectorService.findByNameContaining("不存在的名称XYZ");

        assertNotNull(list);
        assertTrue(list.isEmpty(), "不匹配的关键词应返回空列表");
    }

    @Test
    @Order(7)
    void testUpdate() {
        assertNotNull(savedId, "应先生成测试数据");

        Optional<TestVector> opt = testVectorService.findById(savedId);
        assertTrue(opt.isPresent());

        TestVector entity = opt.get();
        entity.setName("更新后的向量");
        entity.setEmbedding(new float[]{4.0f, 5.0f, 6.0f});

        TestVector updated = testVectorService.save(entity);

        assertEquals(savedId, updated.getId(), "ID 不应改变");
        assertEquals("更新后的向量", updated.getName());
        assertArrayEquals(new float[]{4.0f, 5.0f, 6.0f}, updated.getEmbedding(), 0.001f);
        System.out.println("更新成功: " + updated);
    }

    @Test
    @Order(8)
    void testDeleteById() {
        assertNotNull(savedId, "应先生成测试数据");

        testVectorService.deleteById(savedId);

        Optional<TestVector> result = testVectorService.findById(savedId);
        assertFalse(result.isPresent(), "删除后应查不到数据");
        System.out.println("删除成功，ID = " + savedId);

        savedId = null;
    }

    // ==================== Repository 层原生查询测试 ====================

    @Test
    @Order(9)
    void testFindByNameNative() {
        // 先插入一条临时数据
        TestVector vector = new TestVector();
        vector.setName("原生查询测试");
        vector.setEmbedding(new float[]{7.0f, 8.0f, 9.0f});
        TestVector saved = testVectorRepository.save(vector);

        List<TestVector> list = testVectorRepository.findByNameNative("原生查询测试");

        assertFalse(list.isEmpty(), "原生 SQL 查询应返回结果");
        assertEquals("原生查询测试", list.get(0).getName());
        System.out.println("findByNameNative 查到 " + list.size() + " 条");

        // 清理
        testVectorRepository.deleteById(saved.getId());
    }

    @Test
    @Order(10)
    void testFindById_notExist() {
        Optional<TestVector> result = testVectorService.findById(-999);

        assertFalse(result.isPresent(), "不存在的 ID 应返回空");
    }
}
