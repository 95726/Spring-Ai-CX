-- =====================================================
-- MySQL 数据库表结构定义文件
-- 数据库: mysql_ai
-- 说明: 此文件记录所有表结构，便于维护和查阅
-- =====================================================

-- =====================================================
-- 表说明模板
-- =====================================================
-- CREATE TABLE `table_name` (
--     `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
--     `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
--     `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
--     PRIMARY KEY (`id`)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表注释';

-- =====================================================
-- 以下为实际表结构定义
-- =====================================================
CREATE TABLE `AI_USER` (
                           `id` VARCHAR(32) NOT NULL COMMENT '主键ID(32位UUID)',
                           `user_bh` VARCHAR(32) NOT NULL COMMENT '用户编号',
                           `user_name` VARCHAR(256) NOT NULL COMMENT '用户名称',
                           `password` VARCHAR(128) NOT NULL COMMENT '登录密码',
                           `phone` VARCHAR(20) DEFAULT NULL COMMENT '电话号码',
                           `status` VARCHAR(16) DEFAULT 1 COMMENT '用户状态',
                           `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           `create_name` VARCHAR(32) DEFAULT NULL COMMENT '创建人',
                           `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                           `update_name` VARCHAR(32) DEFAULT NULL COMMENT '更新人',
                           PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI用户表';