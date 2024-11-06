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
@TableName("video")
@Schema(name = "Video", description = "视频表")
public class Video {

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "视频标题")
    private String title;

    @Schema(description = "视频描述")
    private String description;

    @Schema(description = "视频封面")
    private String cover;

    @Schema(description = "视频链接")
    private String url;

    @Schema(description = "上传者")
    private String userId;

    @Schema(description = "上传时间")
    private LocalDateTime uploadTime;

    @Schema(description = "播放次数")
    private Long views;

    @Schema(description = "点赞次数")
    private Long likes;

    @Schema(description = "点踩次数")
    private Long dislikes;

    @Schema(description = "状态 0为正常 1为已被禁用 2为已被删除")
    private Integer status;
}
