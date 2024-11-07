package com.sky.web.service.impl;

import com.google.gson.Gson;
import com.sky.common.utils.SseEmitterUtil;
import com.sky.pojo.constant.InternalMessageSendTypeConstants;
import com.sky.pojo.constant.InternalMessageTypeConstants;
import com.sky.pojo.entity.InternalMessage;
import com.sky.pojo.mapper.InternalMessageMapper;
import com.sky.pojo.vo.SseVo;
import com.sky.web.service.InternalMessageService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class InternalMessageServiceImpl implements InternalMessageService {

    @Resource
    private InternalMessageMapper internalMessageMapper;
    @Resource
    Gson gson;

    /**
     * 发送点赞消息
     */
    @Override
    public void sendLikeMessage(Long receiverUserId, Long receiverId,
                                Integer receiverType, Long userId) {
        InternalMessage internalMessage = new InternalMessage()
                .setReceiverUserId(receiverUserId)
                .setType(InternalMessageTypeConstants.LIKE)
                .setReceiverId(receiverId)
                .setReceiverType(receiverType)
                .setUserId(userId)
                .setRead(false)
                .setCreateTime(LocalDateTime.now());
        internalMessageMapper.insert(internalMessage);

        SseEmitterUtil.sendMessage(receiverUserId.toString(), gson.toJson(new SseVo().setType("like")));
    }

    /**
     * 发送点踩消息
     */
    @Override
    public void sendDislikeMessage(Long receiverUserId, Long receiverId,
                                   Integer receiverType, Long userId) {
        InternalMessage internalMessage = new InternalMessage()
                .setReceiverUserId(receiverUserId)
                .setReceiverId(receiverId)
                .setReceiverType(receiverType)
                .setType(InternalMessageTypeConstants.DISLIKE)
                .setUserId(userId)
                .setRead(false)
                .setCreateTime(LocalDateTime.now());
        internalMessageMapper.insert(internalMessage);
    }

    /**
     * 发送@消息
     */
    @Override
    public void sendMentionMessage(Long receiverUserId, Long receiverId,
                                   Integer receiverType, Long sendId, Long userId) {
        InternalMessage internalMessage = new InternalMessage()
                .setReceiverUserId(receiverUserId)
                .setReceiverId(receiverId)
                .setReceiverType(receiverType)
                .setType(InternalMessageTypeConstants.MENTION)
                .setSendType(InternalMessageSendTypeConstants.COMMENT)
                .setSendId(sendId)
                .setUserId(userId)
                .setRead(false)
                .setCreateTime(LocalDateTime.now());
        internalMessageMapper.insert(internalMessage);

        SseEmitterUtil.sendMessage(receiverUserId.toString(), gson.toJson(new SseVo().setType("mention")));
    }
    /**
     * 发送系统消息
     */
    @Override
    public void sendSystemMessage(Long receiverUserId, String message, Long userId) {
        InternalMessage internalMessage = new InternalMessage()
                .setReceiverUserId(receiverUserId)
                .setType(InternalMessageTypeConstants.SYSTEM)
                .setContent(message)
                .setRead(false)
                .setCreateTime(LocalDateTime.now());
        internalMessageMapper.insert(internalMessage);

        SseEmitterUtil.sendMessage(receiverUserId.toString(), gson.toJson(new SseVo().setType("system")));
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
                .setSendType(targetType)
                .setSendId(targetId)
                .setUserId(userId)
                .setContent(comment)
                .setRead(false)
                .setCreateTime(LocalDateTime.now());
        internalMessageMapper.insert(internalMessage);

        SseEmitterUtil.sendMessage(receiverUserId.toString(), gson.toJson(new SseVo().setType("comment")));
    }

}