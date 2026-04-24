package com.example.springai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springai.entity.AiUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI用户Mapper接口
 *
 * 继承MyBatis-Plus的BaseMapper，提供基础的CRUD操作
 */
@Mapper
public interface AiUserMapper extends BaseMapper<AiUser> {

}