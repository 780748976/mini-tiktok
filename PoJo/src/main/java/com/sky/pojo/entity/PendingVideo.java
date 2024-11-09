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
@TableName("pending_video")
@Schema(name = "PendingVideo", description = "待审核视频表")
public class PendingVideo {

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "视频标题")
    private String title;

    @Schema(description = "视频封面")
    private String cover;

    @Schema(description = "视频描述")
    private String description;

    @Schema(description = "视频链接")
    private String url;

    @Schema(description = "上传者")
    private Long userId;

    @Schema(description = "上传时间")
    private LocalDateTime uploadTime;

    @Schema(description = "审核状态")
    private String auditStatus;

    @Schema(description = "审核意见")
    private String auditComment;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "审核者")
    private Long auditUserId;

    @Schema(description = "审核者用户名")
    private String auditUsername;

    @Schema(description = "是否已审核")
    private Boolean isAudited;
}