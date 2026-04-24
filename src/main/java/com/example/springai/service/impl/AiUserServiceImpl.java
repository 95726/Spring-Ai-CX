package com.example.springai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springai.entity.AiUser;
import com.example.springai.mapper.AiUserMapper;
import com.example.springai.service.AiUserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI用户服务实现类
 *
 * 继承MyBatis-Plus的ServiceImpl，实现基础CRUD操作及用户认证方法
 */
@Service
public class AiUserServiceImpl extends ServiceImpl<AiUserMapper, AiUser> implements AiUserService {

    /**
     * BCrypt密码加密器
     */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户注册
     *
     * 注册新用户，密码使用BCrypt加密存储。
     * 注册时自动生成UUID作为主键和用户编号。
     *
     * @param userName 用户名
     * @param password 密码（原始密码）
     * @param phone 电话号码
     * @return 注册成功后的用户对象
     * @throws RuntimeException 如果用户名已存在
     */
    @Override
    public AiUser register(String userName, String password, String phone) {
        // 检查用户名是否已存在
        AiUser existUser = findByUserName(userName);
        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 创建新用户
        AiUser user = new AiUser();
        // 生成UUID作为主键
        user.setId(UUID.randomUUID().toString().replace("-", ""));
        // 生成用户编号
        user.setUserBh(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        user.setUserName(userName);
        // 使用BCrypt加密密码
        user.setPassword(passwordEncoder.encode(password));
        user.setPhone(phone);
        // 设置用户状态为正常
        user.setStatus("1");
        // 设置创建时间和更新时间
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 保存用户
        save(user);
        return user;
    }

    /**
     * 用户登录
     *
     * 验证用户名和密码，密码使用BCrypt进行匹配验证。
     *
     * @param userName 用户名
     * @param password 密码（原始密码）
     * @return 登录成功的用户对象
     * @throws RuntimeException 如果用户名不存在或密码错误
     */
    @Override
    public AiUser login(String userName, String password) {
        // 根据用户名查询用户
        AiUser user = findByUserName(userName);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // 检查用户状态
        if (!"1".equals(user.getStatus())) {
            throw new RuntimeException("用户已被禁用");
        }

        return user;
    }

    /**
     * 根据用户名查询用户
     *
     * 使用LambdaQueryWrapper进行条件查询。
     *
     * @param userName 用户名
     * @return 用户对象，不存在则返回null
     */
    @Override
    public AiUser findByUserName(String userName) {
        LambdaQueryWrapper<AiUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiUser::getUserName, userName);
        return getOne(wrapper);
    }
}