package com.sky.web.service;

import java.util.List;

/**
 * 站内信服务接口
 */
public interface InternalMessageService {

    void sendLikeMessage(Long receiverUserId, Long receiverId,
                         Integer receiverType, Long userId);

    void sendMentionMessage(Long receiverUserId, Long receiverId,
                            Integer receiverType, Long targetId, Long userId, String content);

    void sendSystemMessage(Long receiverUserId, String message);

    void sendSystemMessage(Long receiverUserId, String message, Long receiverId, Integer receiverType);

    void sendCommentMessage(Long receiverUserId, Long receiverId,
                            Integer receiverType, Long targetId, Long userId, String comment);

    void sendDynamicsMessage(List<Long> receiverUserId);
}