package com.sky.web.service;

import com.sky.common.utils.Result;
import org.springframework.transaction.annotation.Transactional;

/**
 * 站内信服务接口
 */
public interface InternalMessageService {

    void sendLikeMessage(Long receiverId, Integer targetType, Long targetId, Long userId);

    void sendDislikeMessage(Long receiverId, Integer targetType, Long targetId, Long userId);

    void sendMentionMessage(Long receiverId, Long targetId, Long userId);
}