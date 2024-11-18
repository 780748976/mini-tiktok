package com.sky.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.common.utils.Result;
import com.sky.pojo.constant.InternalMessageReceiverType;
import com.sky.pojo.constant.InternalMessageTypeConstants;
import com.sky.pojo.dto.CommentParam;
import com.sky.pojo.entity.Comment;
import com.sky.pojo.entity.Video;
import com.sky.pojo.mapper.CommentMapper;
import com.sky.pojo.mapper.VideoMapper;
import com.sky.web.service.CommentService;
import com.sky.web.service.InternalMessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;

import java.time.LocalDateTime;

@Service
public class CommentServiceImpl implements CommentService {

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private InternalMessageService internalMessageService;

    @Resource
    VideoMapper videoMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result addComment(CommentParam commentParam, Long userId) {
        Long parentId = commentParam.getParentId();
        Long videoId = commentParam.getVideoId();
        String content = commentParam.getContent();

        Comment comment = new Comment()
                .setVideoId(videoId)
                .setParentId(parentId)
                .setUserId(userId)
                .setContent(content)
                .setCreateTime(LocalDateTime.now());
        commentMapper.insert(comment);

        if (parentId != null) {
            LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Comment::getId, parentId).select(Comment::getUserId);
            Comment parentComment = commentMapper.selectOne(queryWrapper);
            if (parentComment == null) {
                return Result.failed("父评论不存在");
            }
            // 发送站内信
            internalMessageService.sendCommentMessage(parentComment.getUserId(), parentId,
                    InternalMessageReceiverType.COMMENT, InternalMessageTypeConstants.COMMENT,
                    comment.getId(), userId, content);
        } else {
            LambdaQueryWrapper<Video> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Video::getId, videoId).select(Video::getUserId);
            Video video = videoMapper.selectOne(queryWrapper);
            if (video == null) {
                return Result.failed("视频不存在");
            }
            // 发送站内信
            internalMessageService.sendCommentMessage(video.getUserId(), videoId,
                    InternalMessageReceiverType.VIDEO, InternalMessageTypeConstants.COMMENT,
                    comment.getId(), userId, content);
        }
        //处理@消息
        if (commentParam.getMentionUserIds() != null && !commentParam.getMentionUserIds().isEmpty()) {
            for (String mentionUserId : commentParam.getMentionUserIds()) {
                internalMessageService.sendMentionMessage(Long.parseLong(mentionUserId), parentId,
                    InternalMessageReceiverType.COMMENT,
                    comment.getId(), userId, content);
            }
        }

        return Result.success("评论添加成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result likeComment(Long commentId, Long userId) {
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getId, commentId).eq(Comment::getUserId, userId);
        Comment comment = commentMapper.selectOne(queryWrapper);
        if (comment == null) {
            return Result.failed("评论不存在");
        }
        comment.setLikes(comment.getLikes() + 1);
        commentMapper.updateById(comment);

        // 发送站内信
        internalMessageService.sendLikeMessage(comment.getUserId(), comment.getId(),
                InternalMessageReceiverType.COMMENT, userId);
        return Result.success("点赞成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result dislikeComment(Long commentId, Long userId) {
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getId, commentId).eq(Comment::getUserId, userId);
        Comment comment = commentMapper.selectOne(queryWrapper);
        if (comment == null) {
            return Result.failed("评论不存在");
        }
        comment.setDislikes(comment.getDislikes() + 1);
        commentMapper.updateById(comment);

        return Result.success("点踩成功");
    }
}