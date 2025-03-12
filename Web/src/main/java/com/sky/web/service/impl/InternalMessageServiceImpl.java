package com.sky.web.service.impl;

import com.google.gson.Gson;
import com.sky.common.utils.SseEmitterUtil;
import com.sky.pojo.constant.InternalMessageTypeConstants;
import com.sky.pojo.constant.WebRedisConstants;
import com.sky.pojo.entity.InternalMessage;
import com.sky.pojo.mapper.InternalMessageMapper;
import com.sky.pojo.vo.SseVo;
import com.sky.web.service.InternalMessageService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InternalMessageServiceImpl implements InternalMessageService {

    @Resource
    private InternalMessageMapper internalMessageMapper;
    @Resource
    Gson gson;
    @Resource
    StringRedisTemplate stringRedisTemplate;

    /**
     * 发送点赞消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendLikeMessage(Long receiverUserId, Long receiverId,
                                Integer receiverType, Long userId) {
        InternalMessage internalMessage = new InternalMessage()
                .setReceiverUserId(receiverUserId)
                .setType(InternalMessageTypeConstants.LIKE)
                .setReceiverId(receiverId)
                .setReceiverType(receiverType)
                .setUserId(userId)
                .setIsRead(false)
                .setCreateTime(LocalDateTime.now());
        internalMessageMapper.insert(internalMessage);

        stringRedisTemplate.opsForValue().set(WebRedisConstants.LIKE_MESSAGE + receiverId, "1");
        SseEmitterUtil.sendMessage(receiverUserId.toString(), gson.toJson(new SseVo().setType("like")));
    }

    /**
     * 发送@消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendMentionMessage(Long receiverUserId, Long receiverId,
                                   Integer receiverType, Long sendId, Long userId, String content) {
        InternalMessage internalMessage = new InternalMessage()
                .setReceiverUserId(receiverUserId)
                .setReceiverId(receiverId)
                .setReceiverType(receiverType)
                .setType(InternalMessageTypeConstants.MENTION)
                .setSendId(sendId)
                .setUserId(userId)
                .setIsRead(false)
                .setCreateTime(LocalDateTime.now());
        internalMessageMapper.insert(internalMessage);

        stringRedisTemplate.opsForValue().set(WebRedisConstants.MENTION_MESSAGE + receiverId, "1");
        SseEmitterUtil.sendMessage(receiverUserId.toString(), gson.toJson(new SseVo().setType("mention")));
    }
    /**
     * 发送系统消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendSystemMessage(Long receiverUserId, String message) {
        InternalMessage internalMessage = new InternalMessage()
                .setReceiverUserId(receiverUserId)
                .setType(InternalMessageTypeConstants.SYSTEM)
                .setContent(message)
                .setIsRead(false)
                .setCreateTime(LocalDateTime.now());
        internalMessageMapper.insert(internalMessage);

        stringRedisTemplate.opsForValue().set(WebRedisConstants.SYSTEM_MESSAGE + receiverUserId, "1");
        SseEmitterUtil.sendMessage(receiverUserId.toString(), gson.toJson(new SseVo().setType("system")));
    }

    /**
     * 发送系统消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendSystemMessage(Long receiverUserId, String message, Long receiverId, Integer receiverType) {
        InternalMessage internalMessage = new InternalMessage()
                .setReceiverUserId(receiverUserId)
                .setType(InternalMessageTypeConstants.SYSTEM)
                .setContent(message)
                .setReceiverId(receiverId)
                .setReceiverType(receiverType)
                .setIsRead(false)
                .setCreateTime(LocalDateTime.now());
        internalMessageMapper.insert(internalMessage);

        stringRedisTemplate.opsForValue().set(WebRedisConstants.SYSTEM_MESSAGE + receiverUserId, "1");
        SseEmitterUtil.sendMessage(receiverUserId.toString(), gson.toJson(new SseVo().setType("system")));
    }

    /**
     * 发送评论消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendCommentMessage(Long receiverUserId, Long receiverId,
                                   Integer receiverType, Long targetId, Long userId, String comment) {
        InternalMessage internalMessage = new InternalMessage()
                .setReceiverUserId(receiverUserId)
                .setReceiverId(receiverId)
                .setReceiverType(receiverType)
                .setType(InternalMessageTypeConstants.COMMENT)
                .setSendId(targetId)
                .setUserId(userId)
                .setContent(comment)
                .setIsRead(false)
                .setCreateTime(LocalDateTime.now());
        internalMessageMapper.insert(internalMessage);

        stringRedisTemplate.opsForValue().set(WebRedisConstants.COMMENT_MESSAGE + receiverId, "1");
        SseEmitterUtil.sendMessage(receiverUserId.toString(), gson.toJson(new SseVo().setType("comment")));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendDynamicsMessage(List<Long> receiverUserId) {
        receiverUserId.forEach(userId -> {
            stringRedisTemplate.opsForValue().set(WebRedisConstants.DYNAMIC_MESSAGE + userId, "1");
            SseEmitterUtil.sendMessage(userId.toString(), gson.toJson(new SseVo().setType("dynamics")));
        });
    }

}