package com.sky.pojo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.pojo.entity.UserInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 用户信息表 Mapper 接口
 * </p>
 *
 * @author lin
 * @since 2024-07-22 06:15:13
 */
@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {
}
