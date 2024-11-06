package com.sky.web.service.impl;

import com.sky.common.utils.Result;
import com.sky.pojo.constant.InternalMessageTypeConstants;
import com.sky.pojo.dto.CommentParam;
import com.sky.pojo.entity.Comment;
import com.sky.pojo.entity.InternalMessage;
import com.sky.pojo.mapper.CommentMapper;
import com.sky.pojo.mapper.InternalMessageMapper;
import com.sky.web.service.CommentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class CommentServiceImpl implements CommentService {

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private InternalMessageMapper internalMessageMapper;

    @Override
    @Transactional
    public Result addComment(CommentParam commentParam) {
        Comment comment = new Comment()
                .setVideoId(commentParam.getVideoId())
                .setUserId(commentParam.getUserId())
                .setContent(commentParam.getContent())
                .setCreateTime(LocalDateTime.now());
        commentMapper.insert(comment);


        return Result.success("评论添加成功");
    }

    @Override
    @Transactional
    public Result replyComment(CommentParam commentParam) {
        Comment comment = new Comment()
                .setVideoId(commentParam.getVideoId())
                .setParentId(commentParam.getParentId())
                .setUserId(commentParam.getUserId())
                .setContent(commentParam.getContent())
                .setCreateTime(LocalDateTime.now());
        commentMapper.insert(comment);
        return Result.success("回复添加成功");
    }

    @Override
    @Transactional
    public Result likeComment(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            return Result.failed("评论不存在");
        }
        comment.setLikes(comment.getLikes() + 1);
        commentMapper.updateById(comment);

        // 发送站内信


        return Result.success("点赞成功");
    }

    @Override
    @Transactional
    public Result dislikeComment(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            return Result.failed("评论不存在");
        }
        comment.setDislikes(comment.getDislikes() + 1);
        commentMapper.updateById(comment);

        // 发送站内信


        return Result.success("点踩成功");
    }
}