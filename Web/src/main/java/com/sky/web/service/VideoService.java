package com.sky.web.service;

import com.sky.common.utils.Result;
import com.sky.pojo.dto.UploadPendingVideoParam;
import com.sky.pojo.entity.Video;
import com.sky.pojo.vo.UserVideo;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public interface VideoService {
    Result getUserVideoList(Long userId, Integer page, Integer size);

    Result uploadPendingVideo(UploadPendingVideoParam pendingVideo, Long userId);

    Result viewVideo(Long videoId, Long userId) throws ExecutionException, InterruptedException;

    Result likeVideo(Long videoId, Long userId);

    List<UserVideo> fillVideo(List<Video> videos, Long userId) throws ExecutionException, InterruptedException;

    Result dislikeVideo(Long videoId, Long userId);

    Result favoriteVideo(Long videoId, Long userId);

    Result unfavoriteVideo(Long videoId, Long userId);

    Result getFavoriteVideoList(Long userId, Integer page, Integer size);

    Result getBrowseRecordList(Long userId, Integer page, Integer size);

    Result searchVideo(String keyword, Integer page, Integer size, String sortType) throws IOException;

    Result getUserLikeVideoList(Long userId, Integer page, Integer size);
}
