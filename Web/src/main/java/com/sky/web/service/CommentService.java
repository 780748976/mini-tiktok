package com.sky.web.service;

import com.sky.common.utils.Result;
import com.sky.pojo.dto.CommentParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

public interface CommentService {

    Result addComment(@RequestBody @Validated CommentParam commentParam);

    Result replyComment(@RequestBody @Validated CommentParam commentParam);

    Result likeComment(@RequestParam Long commentId);

    Result dislikeComment(@RequestParam Long commentId);
}