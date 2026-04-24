package com.example.springai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springai.entity.AiUser;
import com.example.springai.mapper.AiUserMapper;
import com.example.springai.service.AiUserService;
import org.springframework.stereotype.Service;

/**
 * AI用户服务实现类
 *
 * 继承MyBatis-Plus的ServiceImpl，实现基础CRUD操作
 */
@Service
public class AiUserServiceImpl extends ServiceImpl<AiUserMapper, AiUser> implements AiUserService {

}