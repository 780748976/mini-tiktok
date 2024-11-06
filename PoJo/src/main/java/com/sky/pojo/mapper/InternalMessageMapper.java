package com.sky.pojo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.pojo.entity.InternalMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface InternalMessageMapper extends BaseMapper<InternalMessage> {
    //可合并消息 人数加一 更新时间
    @Update("update internal_message set people = people + 1, update_time = #{updateTime} where id = #{id}")
    void mergeMessage(Long id, LocalDateTime updateTime);
}