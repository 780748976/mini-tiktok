package com.sky.web.service;

import com.sky.common.utils.Result;
import com.sky.pojo.dto.CommentParam;
import org.springframework.stereotype.Service;

@Service
public interface CommentService {

    Result addComment(CommentParam commentParam, Long userId);

    Result likeComment(Long commentId, Long userId);

    Result dislikeComment(Long commentId, Long userId);
    
    /**
     * 分页查询视频下的评论，每个父评论最多显示3条子评论
     *
     * @param videoId 视频ID
     * @param page 当前页码
     * @param size 每页大小
     * @param userId 当前用户ID，用于判断是否点赞/点踩
     * @return 评论列表
     */
    Result getVideoComments(Long videoId, Integer page, Integer size, Long userId);
    
    /**
     * 分页查询评论下的子评论
     *
     * @param commentId 父评论ID
     * @param page 当前页码
     * @param size 每页大小
     * @param userId 当前用户ID，用于判断是否点赞/点踩
     * @return 子评论列表
     */
    Result getChildComments(Long commentId, Integer page, Integer size, Long userId);
}