package com.sky.web.service.impl;

import com.sky.pojo.constant.InternalMessageTargetTypeConstants;
import com.sky.pojo.constant.InternalMessageTypeConstants;
import com.sky.pojo.entity.InternalMessage;
import com.sky.pojo.mapper.InternalMessageMapper;
import com.sky.web.service.InternalMessageService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class InternalMessageServiceImpl implements InternalMessageService {

    @Resource
    private InternalMessageMapper internalMessageMapper;

    /**
     * 发送点赞消息
     */
    @Override
    public void sendLikeMessage(Long receiverUserId, Long receiverId,
                                Integer receiverType, Integer targetType, Long targetId, Long userId) {
        InternalMessage internalMessage = new InternalMessage()
                .setReceiverUserId(receiverUserId)
                .setType(InternalMessageTypeConstants.LIKE)
                .setReceiverId(receiverId)
                .setReceiverType(receiverType)
                .setTargetType(targetType)
                .setTargetId(targetId)
                .setUserId(userId)
                .setRead(false)
                .setCreateTime(LocalDateTime.now());
        internalMessageMapper.insert(internalMessage);
    }

    /**
     * 发送点踩消息
     */
    @Override
    public void sendDislikeMessage(Long receiverUserId,Long receiverId,
                                   Integer receiverType, Integer targetType, Long targetId, Long userId) {
        InternalMessage internalMessage = new InternalMessage()
                .setReceiverUserId(receiverUserId)
                .setReceiverId(receiverId)
                .setReceiverType(receiverType)
                .setType(InternalMessageTypeConstants.DISLIKE)
                .setTargetType(targetType)
                .setTargetId(targetId)
                .setUserId(userId)
                .setRead(false)
                .setCreateTime(LocalDateTime.now());
        internalMessageMapper.insert(internalMessage);
    }

    /**
     * 发送@消息
     */
    @Override
    public void sendMentionMessage(Long receiverUserId,Long receiverId,
                                   Integer receiverType, Long targetId, Long userId) {
        InternalMessage internalMessage = new InternalMessage()
                .setReceiverUserId(receiverUserId)
                .setReceiverId(receiverId)
                .setReceiverType(receiverType)
                .setType(InternalMessageTypeConstants.MENTION)
                .setTargetType(InternalMessageTargetTypeConstants.COMMENT)
                .setTargetId(targetId)
                .setUserId(userId)
                .setRead(false)
                .setCreateTime(LocalDateTime.now());
        internalMessageMapper.insert(internalMessage);
    }
    /**
     * 发送系统消息
     */
    @Override
    public void sendSystemMessage(Long receiverId, String message, Long userId) {
        InternalMessage internalMessage = new InternalMessage()
                .setReceiverId(receiverId)
                .setType(InternalMessageTypeConstants.SYSTEM)
                .setContent(message)
                .setRead(false)
                .setCreateTime(LocalDateTime.now());
        internalMessageMapper.insert(internalMessage);
    }

    /**
     * 发送评论消息
     */
    @Override
    public void sendCommentMessage(Long receiverUserId, Long receiverId,
                                   Integer receiverType, Integer targetType, Long targetId, Long userId, String comment) {
        InternalMessage internalMessage = new InternalMessage()
                .setReceiverUserId(receiverUserId)
                .setReceiverId(receiverId)
                .setReceiverType(receiverType)
                .setType(InternalMessageTypeConstants.COMMENT)
                .setTargetType(targetType)
                .setTargetId(targetId)
                .setUserId(userId)
                .setContent(comment)
                .setRead(false)
                .setCreateTime(LocalDateTime.now());
        internalMessageMapper.insert(internalMessage);
    }

}