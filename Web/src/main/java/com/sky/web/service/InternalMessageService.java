package com.sky.web.service;

import com.sky.common.utils.Result;
import org.springframework.transaction.annotation.Transactional;

/**
 * 站内信服务接口
 */
public interface InternalMessageService {

    void sendLikeMessage(Long receiverUserId, Long receiverId,
                         Integer receiverType, Long userId);

    void sendDislikeMessage(Long receiverUserId, Long receiverId,
                            Integer receiverType, Long userId);

    void sendMentionMessage(Long receiverUserId, Long receiverId,
                            Integer receiverType, Long targetId, Long userId);

    void sendSystemMessage(Long receiverId, String message, Long userId);

    void sendCommentMessage(Long receiverUserId, Long receiverId,
                            Integer receiverType, Integer targetType, Long targetId, Long userId, String comment);
}