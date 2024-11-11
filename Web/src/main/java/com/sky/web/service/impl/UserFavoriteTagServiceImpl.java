package com.sky.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.pojo.entity.UserFavoriteTag;
import com.sky.pojo.entity.VideoTag;
import com.sky.pojo.mapper.TagMapper;
import com.sky.pojo.mapper.UserFavoriteTagMapper;
import com.sky.pojo.mapper.VideoTagMapper;
import com.sky.web.service.UserFavoriteTagService;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;

import java.util.List;

@Service
public class UserFavoriteTagServiceImpl implements UserFavoriteTagService {

    @Resource
    UserFavoriteTagMapper userFavoriteTagMapper;
    @Resource
    TagMapper tagMapper;
    @Resource
    VideoTagMapper videoTagMapper;

    @Override
    public void viewVideoAddProbability(Long videoId, Long userId) {
        List<VideoTag> videoTags = videoTagMapper.selectList(
                new LambdaQueryWrapper<VideoTag>().eq(VideoTag::getVideoId, videoId));
        LambdaQueryWrapper<UserFavoriteTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserFavoriteTag::getUserId, userId)
                .in(UserFavoriteTag::getTagId, videoTags.stream().map(VideoTag::getTagId).toArray());
        List<UserFavoriteTag> userFavoriteTags = userFavoriteTagMapper.selectList(queryWrapper);
        userFavoriteTags.forEach(userFavoriteTag -> {
            userFavoriteTag.setProbability(userFavoriteTag.getProbability() + 1);
        });
        userFavoriteTagMapper.batchUpdate(userFavoriteTags);
    }

}