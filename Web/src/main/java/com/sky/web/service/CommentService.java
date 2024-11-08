package com.sky.web.service;

import com.sky.common.utils.Result;
import com.sky.pojo.dto.CommentParam;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Service
public interface CommentService {

    Result addComment(CommentParam commentParam);

    Result replyComment(CommentParam commentParam);

    Result likeComment(Long commentId);

    Result dislikeComment(Long commentId);
}