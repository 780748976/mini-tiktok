package com.sky.pojo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.pojo.entity.VideoTag;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface VideoTagMapper extends BaseMapper<VideoTag> {

    @Insert("<script>" +
            "insert into video_tag (video_id, tag_id) values " +
            "<foreach collection='tagList' item='tag' separator=','>" +
            "(#{id}, (select id from tag where name = #{tag}))" +
            "</foreach>" +
            "</script>")
    void insertBatch(Long id, List<String> tagList);
}
