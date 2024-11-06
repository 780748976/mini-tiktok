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
@TableName("video_sub_comment")
@Schema(name = "VideoSubComment", description = "视频子评论表")
public class VideoSubComment {

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "评论ID")
    private Long commentId;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "点赞次数")
    private Long likes;

    @Schema(description = "点踩次数")
    private Long dislikes;

    @Schema(description = "回复次数")
    private Long replys;

    @Schema(description = "评论状态")
    private String status;

    @Schema(description = "评论时间")
    private LocalDateTime commentTime;

    @Schema(description = "父评论ID")
    private Long parentId;
}