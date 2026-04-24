package com.example.springai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springai.entity.AiUser;
import com.example.springai.service.AiUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI用户控制器
 *
 * 提供用户的增删改查接口
 */
@RestController
@RequestMapping("/api/user")
public class AiUserController {

    @Autowired
    private AiUserService aiUserService;

    /**
     * 保存用户
     *
     * @param aiUser 用户信息
     * @return 是否保存成功
     */
    @PostMapping("/save")
    public boolean save(@RequestBody AiUser aiUser) {
        return aiUserService.save(aiUser);
    }

    /**
     * 批量保存用户
     *
     * @param aiUsers 用户列表
     * @return 是否保存成功
     */
    @PostMapping("/saveBatch")
    public boolean saveBatch(@RequestBody List<AiUser> aiUsers) {
        return aiUserService.saveBatch(aiUsers);
    }

    /**
     * 根据ID删除用户
     *
     * @param id 用户ID
     * @return 是否删除成功
     */
    @DeleteMapping("/delete/{id}")
    public boolean delete(@PathVariable String id) {
        return aiUserService.removeById(id);
    }

    /**
     * 批量删除用户
     *
     * @param ids 用户ID列表
     * @return 是否删除成功
     */
    @DeleteMapping("/deleteBatch")
    public boolean deleteBatch(@RequestBody List<String> ids) {
        return aiUserService.removeByIds(ids);
    }

    /**
     * 更新用户
     *
     * @param aiUser 用户信息
     * @return 是否更新成功
     */
    @PutMapping("/update")
    public boolean update(@RequestBody AiUser aiUser) {
        return aiUserService.updateById(aiUser);
    }

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/get/{id}")
    public AiUser getById(@PathVariable String id) {
        return aiUserService.getById(id);
    }

    /**
     * 查询所有用户
     *
     * @return 用户列表
     */
    @GetMapping("/list")
    public List<AiUser> list() {
        return aiUserService.list();
    }

    /**
     * 分页查询用户
     *
     * @param page 当前页
     * @param size 每页大小
     * @return 分页结果
     */
    @GetMapping("/page")
    public Page<AiUser> page(@RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "10") Integer size) {
        return aiUserService.page(new Page<>(page, size));
    }

    /**
     * 根据用户名查询用户
     *
     * @param userName 用户名
     * @return 用户信息
     */
    @GetMapping("/getByName")
    public AiUser getByUserName(@RequestParam String userName) {
        LambdaQueryWrapper<AiUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiUser::getUserName, userName);
        return aiUserService.getOne(wrapper);
    }
}