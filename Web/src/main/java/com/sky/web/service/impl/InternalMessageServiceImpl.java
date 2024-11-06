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
    public void sendLikeMessage(Long receiverId, Integer targetType, Long targetId, Long userId) {
        InternalMessage internalMessage = new InternalMessage()
                .setReceiverId(receiverId)
                .setType(InternalMessageTypeConstants.LIKE)
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
    public void sendDislikeMessage(Long receiverId, Integer targetType, Long targetId, Long userId) {
        InternalMessage internalMessage = new InternalMessage()
                .setReceiverId(receiverId)
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
    public void sendMentionMessage(Long receiverId, Long targetId, Long userId) {
        InternalMessage internalMessage = new InternalMessage()
                .setReceiverId(receiverId)
                .setType(InternalMessageTypeConstants.MENTION)
                .setTargetType(InternalMessageTargetTypeConstants.COMMENT)
                .setTargetId(targetId)
                .setUserId(userId)
                .setRead(false)
                .setCreateTime(LocalDateTime.now());
        internalMessageMapper.insert(internalMessage);
    }
}