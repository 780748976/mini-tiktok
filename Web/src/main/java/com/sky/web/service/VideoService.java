package com.sky.web.service;

import com.sky.common.utils.Result;
import com.sky.pojo.dto.UploadPendingVideoParam;
import org.springframework.stereotype.Service;

@Service
public interface VideoService {
    Result getUserVideoList(Long userId, Integer page, Integer size);

    Result uploadPendingVideo(UploadPendingVideoParam pendingVideo, String userId);

    Result viewVideo(Long videoId, String userId);

    Result likeVideo(Long videoId, String userId);

    Result dislikeVideo(Long videoId, String userId);
}
