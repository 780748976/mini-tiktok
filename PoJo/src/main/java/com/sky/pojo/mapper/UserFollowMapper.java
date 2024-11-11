package com.sky.pojo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.pojo.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {
}