package com.sky.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("comment")
@Schema(name = "Comment", description = "视频评论表")
public class Comment {
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "视频ID")
    private Long videoId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "父评论ID，顶级评论为null")
    private Long parentId;

    @Schema(description = "回复的二级评论ID")
    private Long replyId;

    @Schema(description = "回复的用户ID")
    private Long replyUserId;

    @Schema(description = "@的用户列表，格式：nickname1:userId1,nickname2:userId2")
    private String mentions;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "点赞次数")
    private Long likes;

    @Schema(description = "点踩次数")
    private Long dislikes;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}