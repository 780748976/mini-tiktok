package com.sky.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
@Schema(name = "CommentVO", description = "视频评论视图对象")
public class CommentVO {

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "用户头像")
    private String avatar;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "点赞次数")
    private Long likes;

    @Schema(description = "点踩次数")
    private Long dislikes;

    @Schema(description = "当前用户是否点赞")
    private Boolean isLiked;

    @Schema(description = "当前用户是否点踩")
    private Boolean isDisliked;

    @Schema(description = "子评论数量")
    private Long childCount;

    @Schema(description = "子评论列表")
    private List<CommentVO> childComments;

    @Schema(description = "评论时间")
    private LocalDateTime createTime;
    
    @Schema(description = "回复的二级评论ID")
    private Long replyId;
    
    @Schema(description = "回复的用户ID")
    private Long replyUserId;
    
    @Schema(description = "回复的用户昵称")
    private String replyUserName;
    
    @Schema(description = "@的用户列表，格式：nickname1:userId1,nickname2:userId2")
    private String mentions;
}