package com.sky.pojo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.pojo.entity.Video;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface VideoMapper extends BaseMapper<Video> {
    //点赞单个字段+1
    @Update("update video set likes = likes + 1 where id = #{videoId}")
    int like(Long videoId);

    //点踩单个字段+1
    @Update("update video set dislikes = dislikes + 1 where id = #{videoId}")
    int dislike(Long videoId);

    //取消点赞单个字段-1
    @Update("update video set likes = likes - 1 where id = #{videoId}")
    int cancelLike(Long videoId);

    //取消点踩单个字段-1
    @Update("update video set dislikes = dislikes - 1 where id = #{videoId}")
    int cancelDislike(Long videoId);
}
