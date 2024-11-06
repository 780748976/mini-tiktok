package com.sky.pojo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 系统管理-用户基础信息表 Mapper 接口
 * </p>
 *
 * @author lin
 * @since 2024-07-22 06:15:13
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
