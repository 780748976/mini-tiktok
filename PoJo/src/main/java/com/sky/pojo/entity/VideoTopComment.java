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
@TableName("video_top_comment")
@Schema(name = "VideoTopComment", description = "视频主评论表")
public class VideoTopComment {

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "视频ID")
    private Long videoId;

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
}