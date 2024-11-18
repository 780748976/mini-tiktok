package com.sky.web.service;

import com.sky.common.utils.Result;
import com.sky.pojo.dto.CommentParam;
import org.springframework.stereotype.Service;

@Service
public interface CommentService {

    Result addComment(CommentParam commentParam, Long userId);

    Result likeComment(Long commentId, Long userId);

    Result dislikeComment(Long commentId, Long userId);
}