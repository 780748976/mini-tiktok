package com.sky.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sky.pojo.constant.UserChangeProbabilityType;
import com.sky.pojo.entity.UserFavoriteTag;
import com.sky.pojo.entity.VideoTag;
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
    VideoTagMapper videoTagMapper;

    //Type: 0 播放 1 点赞 2 取消点赞 3 点踩 4 取消点踩 5 收藏 6 取消收藏 对应UserChangeProbabilityType
    @Override
    public void changeProbability(Long videoId, Long userId, Integer Type) {
        int probability;
        if (Type == UserChangeProbabilityType.PLAY){
            probability = 1;
        }
        else if (Type == UserChangeProbabilityType.LIKE){
            probability = 2;
        }
        else if (Type == UserChangeProbabilityType.UNLIKE){
            probability = -2;
        }
        else if (Type == UserChangeProbabilityType.DISLIKE){
            probability = -2;
        }
        else if (Type == UserChangeProbabilityType.UNDISLIKE){
            probability = 2;
        }
        else if (Type == UserChangeProbabilityType.FAVORITE){
            probability = 3;
        }
        else if (Type == UserChangeProbabilityType.UNFAVORITE){
            probability = -3;
        }
        else if (Type == UserChangeProbabilityType.LIKE_TO_DISLIKE){
            probability = -4;
        }
        else if (Type == UserChangeProbabilityType.DISLIKE_TO_LIKE){
            probability = 4;
        }
        else {
            return;
        }
        List<VideoTag> videoTags = videoTagMapper.selectList(
                new LambdaQueryWrapper<VideoTag>().eq(VideoTag::getVideoId, videoId));
        LambdaQueryWrapper<UserFavoriteTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserFavoriteTag::getUserId, userId)
                .in(UserFavoriteTag::getTagId, videoTags.stream().map(VideoTag::getTagId).toArray());
        List<UserFavoriteTag> userFavoriteTags = userFavoriteTagMapper.selectList(queryWrapper);
        //如果有不存在的标签就创建
        if (userFavoriteTags.size() != videoTags.size()) {
            for (VideoTag videoTag : videoTags) {
                boolean exist = false;
                for (UserFavoriteTag userFavoriteTag : userFavoriteTags) {
                    if (userFavoriteTag.getTagId().equals(videoTag.getTagId())) {
                        exist = true;
                        break;
                    }
                }
                if (!exist) {
                    UserFavoriteTag userFavoriteTag = new UserFavoriteTag();
                    userFavoriteTag.setUserId(userId);
                    userFavoriteTag.setTagId(videoTag.getTagId());
                    userFavoriteTag.setProbability(0);
                    userFavoriteTagMapper.insert(userFavoriteTag);
                    userFavoriteTags.add(userFavoriteTag);
                }
            }
        }
        LambdaUpdateWrapper<UserFavoriteTag> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserFavoriteTag::getUserId, userId)
                .in(UserFavoriteTag::getTagId, userFavoriteTags.stream().map(UserFavoriteTag::getTagId).toArray())
                .setSql("probability = probability + " + probability);
        userFavoriteTagMapper.update(null, updateWrapper);
    }

}