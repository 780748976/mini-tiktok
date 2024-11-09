package com.sky.pojo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.pojo.entity.UserFavoriteTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserFavoriteTagMapper extends BaseMapper<UserFavoriteTag> {

    //批量更新用户喜欢的标签概率
    @Update({
        "<script>",
        "update user_favorite_tag",
        "<set>",
        "<foreach collection='userFavoriteTags' item='item' separator=','>",
        "<if test='item.tagId != null'>",
        "tag_id = #{item.tagId}",
        "</if>",
        "<if test='item.userId != null'>",
        "user_id = #{item.userId}",
        "</if>",
        "<if test='item.probability != null'>",
        "probability = #{item.probability}",
        "</if>",
        "</foreach>",
        "</set>",
        "where user_id = #{userId}",
        "</script>"
    })
    void batchUpdate(List<UserFavoriteTag> userFavoriteTags);
}