package com.sky.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.common.utils.Result;
import com.sky.pojo.constant.InternalMessageReceiverType;
import com.sky.pojo.constant.InternalMessageTypeConstants;
import com.sky.pojo.dto.CommentParam;
import com.sky.pojo.entity.Comment;
import com.sky.pojo.entity.UserInfo;
import com.sky.pojo.entity.Video;
import com.sky.pojo.mapper.CommentMapper;
import com.sky.pojo.mapper.UserInfoMapper;
import com.sky.pojo.mapper.VideoMapper;
import com.sky.pojo.vo.CommentVO;
import com.sky.web.service.CommentService;
import com.sky.web.service.InternalMessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private InternalMessageService internalMessageService;

    @Resource
    private VideoMapper videoMapper;
    
    @Resource
    private UserInfoMapper userInfoMapper;

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
                .setLikes(0L)
                .setDislikes(0L)
                .setCreateTime(LocalDateTime.now());
        
        // 设置回复的评论ID和用户ID
        if (commentParam.getReplyId() != null) {
            comment.setReplyId(commentParam.getReplyId());
        }
        
        if (commentParam.getReplyUserId() != null) {
            comment.setReplyUserId(commentParam.getReplyUserId());
        }
        
        // 格式化@mentions
        if (commentParam.getMentionUserIds() != null && !commentParam.getMentionUserIds().isEmpty()) {
            StringBuilder mentions = new StringBuilder();
            for (String mentionUserId : commentParam.getMentionUserIds()) {
                Long mentionId = Long.parseLong(mentionUserId);
                // 获取用户昵称
                LambdaQueryWrapper<UserInfo> mentionQueryWrapper = new LambdaQueryWrapper<>();
                mentionQueryWrapper.eq(UserInfo::getId, mentionId).select(UserInfo::getNickname);
                UserInfo mentionedUserInfo = userInfoMapper.selectOne(mentionQueryWrapper);
                if (mentionedUserInfo != null) {
                    if (mentions.length() > 0) {
                        mentions.append(",");
                    }
                    mentions.append(mentionedUserInfo.getNickname()).append(":").append(mentionId);
                }
            }
            comment.setMentions(mentions.toString());
        }
        
        commentMapper.insert(comment);

        // 处理回复通知
        if (parentId != null) {
            LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Comment::getId, parentId).select(Comment::getUserId);
            Comment parentComment = commentMapper.selectOne(queryWrapper);
            if (parentComment == null) {
                return Result.failed("父评论不存在");
            }
                    
            // 如果是回复二级评论，且回复的不是父评论作者，则也通知被回复的用户
            if (comment.getReplyId() != null && comment.getReplyUserId() != null 
                    && !comment.getReplyUserId().equals(parentComment.getUserId())) {
                internalMessageService.sendCommentMessage(comment.getReplyUserId(), parentId,
                        InternalMessageReceiverType.COMMENT, InternalMessageTypeConstants.COMMENT,
                        comment.getId(), userId, content);
            }
            else{
                // 发送站内信给父评论作者
            internalMessageService.sendCommentMessage(parentComment.getUserId(), parentId,
            InternalMessageReceiverType.COMMENT, InternalMessageTypeConstants.COMMENT,
            comment.getId(), userId, content);
            }
        } else {
            LambdaQueryWrapper<Video> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Video::getId, videoId).select(Video::getUserId);
            Video video = videoMapper.selectOne(queryWrapper);
            if (video == null) {
                return Result.failed("视频不存在");
            }
            // 发送站内信给视频作者
            internalMessageService.sendCommentMessage(video.getUserId(), videoId,
                    InternalMessageReceiverType.VIDEO, InternalMessageTypeConstants.COMMENT,
                    comment.getId(), userId, content);
        }
        
        //处理@消息
        if (commentParam.getMentionUserIds() != null && !commentParam.getMentionUserIds().isEmpty()) {
            for (String mentionUserId : commentParam.getMentionUserIds()) {
                Long mentionId = Long.parseLong(mentionUserId);
                // 发送@通知给被提及的用户
                internalMessageService.sendMentionMessage(mentionId, parentId,
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
        queryWrapper.eq(Comment::getId, commentId);
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
        queryWrapper.eq(Comment::getId, commentId);
        Comment comment = commentMapper.selectOne(queryWrapper);
        if (comment == null) {
            return Result.failed("评论不存在");
        }
        comment.setDislikes(comment.getDislikes() + 1);
        commentMapper.updateById(comment);

        return Result.success("点踩成功");
    }

    @Override
    public Result getVideoComments(Long videoId, Integer page, Integer size, Long userId) {
        // 查询视频是否存在
        if (videoMapper.selectById(videoId) == null) {
            return Result.failed("视频不存在");
        }
        
        // 查询一级评论（parentId为null的评论）
        Page<Comment> commentPage = new Page<>(page, size);
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getVideoId, videoId)
                .isNull(Comment::getParentId)
                .orderByDesc(Comment::getCreateTime);
        
        Page<Comment> result = commentMapper.selectPage(commentPage, queryWrapper);
        
        List<CommentVO> commentVOList = new ArrayList<>();
        
        if (result.getRecords().isEmpty()) {
            // 返回空列表和分页信息
            Map<String, Object> map = new HashMap<>();
            map.put("comments", commentVOList);
            map.put("total", result.getTotal());
            map.put("pages", result.getPages());
            map.put("current", result.getCurrent());
            map.put("size", result.getSize());
            return Result.success(map);
        }
        
        // 转换为VO并添加用户信息
        for (Comment comment : result.getRecords()) {
            CommentVO commentVO = convertToCommentVO(comment, userId);
            
            // 查询子评论，最多显示3条
            LambdaQueryWrapper<Comment> childQueryWrapper = new LambdaQueryWrapper<>();
            childQueryWrapper.eq(Comment::getParentId, comment.getId())
                    .orderByDesc(Comment::getCreateTime)
                    .last("LIMIT 3");
            List<Comment> childComments = commentMapper.selectList(childQueryWrapper);
            
            // 转换子评论为VO并设置用户信息
            List<CommentVO> childCommentVOList = childComments.stream()
                    .map(childComment -> convertToCommentVO(childComment, userId))
                    .collect(Collectors.toList());
            
            // 查询子评论总数
            LambdaQueryWrapper<Comment> countQueryWrapper = new LambdaQueryWrapper<>();
            countQueryWrapper.eq(Comment::getParentId, comment.getId());
            Long childCount = commentMapper.selectCount(countQueryWrapper);
            
            commentVO.setChildComments(childCommentVOList);
            commentVO.setChildCount(childCount);
            
            commentVOList.add(commentVO);
        }
        
        // 构建返回结果
        Map<String, Object> map = new HashMap<>();
        map.put("comments", commentVOList);
        map.put("total", result.getTotal());
        map.put("pages", result.getPages());
        map.put("current", result.getCurrent());
        map.put("size", result.getSize());
        
        return Result.success(map);
    }

    @Override
    public Result getChildComments(Long commentId, Integer page, Integer size, Long userId) {
        // 检查评论是否存在
        Comment parentComment = commentMapper.selectById(commentId);
        if (parentComment == null) {
            return Result.failed("评论不存在");
        }
        
        // 分页查询子评论
        Page<Comment> commentPage = new Page<>(page, size);
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getParentId, commentId)
                .orderByDesc(Comment::getCreateTime);
        
        Page<Comment> result = commentMapper.selectPage(commentPage, queryWrapper);
        
        List<CommentVO> commentVOList = result.getRecords().stream()
                .map(comment -> convertToCommentVO(comment, userId))
                .collect(Collectors.toList());
        
        // 构建返回结果
        Map<String, Object> map = new HashMap<>();
        map.put("comments", commentVOList);
        map.put("total", result.getTotal());
        map.put("pages", result.getPages());
        map.put("current", result.getCurrent());
        map.put("size", result.getSize());
        
        return Result.success(map);
    }
    
    /**
     * 将Comment实体转换为CommentVO
     *
     * @param comment 评论实体
     * @param userId 当前用户ID，用于判断是否点赞/点踩
     * @return CommentVO
     */
    private CommentVO convertToCommentVO(Comment comment, Long userId) {
        CommentVO commentVO = new CommentVO();
        commentVO.setId(comment.getId());
        commentVO.setUserId(comment.getUserId());
        commentVO.setContent(comment.getContent());
        commentVO.setLikes(comment.getLikes());
        commentVO.setDislikes(comment.getDislikes());
        commentVO.setCreateTime(comment.getCreateTime());
        
        // 设置回复相关信息
        commentVO.setReplyId(comment.getReplyId());
        commentVO.setReplyUserId(comment.getReplyUserId());
        commentVO.setMentions(comment.getMentions());
        
        // 如果有回复的用户ID，设置回复用户的昵称
        if (comment.getReplyUserId() != null) {
            UserInfo replyUserInfo = userInfoMapper.selectById(comment.getReplyUserId());
            if (replyUserInfo != null) {
                commentVO.setReplyUserName(replyUserInfo.getNickname());
            }
        }
        
        // 设置是否点赞/点踩，后续可扩展
        commentVO.setIsLiked(false);
        commentVO.setIsDisliked(false);
        
        // 获取用户信息
        UserInfo userInfo = userInfoMapper.selectById(comment.getUserId());
        if (userInfo != null) {
            commentVO.setNickname(userInfo.getNickname());
            commentVO.setAvatar(userInfo.getAvatar());
        }
        
        return commentVO;
    }
}