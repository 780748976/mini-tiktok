package com.sky.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("internal_message")
@Schema(name = "InternalMessage", description = "站内信表")
public class InternalMessage {
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "接收用户ID")
    private Long receiverUserId;

    @Schema(description = "接收资源ID 0为评论 1为视频 对应InternalMessageReceiverType")
    private Long receiverId;

    @Schema(description = "接收资源类型")
    private Integer receiverType;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "消息类型 0为系统消息 1为点赞 2为点踩 3为@ 4为评论 对应InternalMessageTypeConstants")
    private Integer type;

    @Schema(description = "发送相关资源类型 0为审核视频 1为视频 2为评论 对应InternalMessageSendTypeConstants")
    private Integer sendType;

    @Schema(description = "发送相关资源ID（如视频ID, 评论ID）")
    private Long sendId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "是否已读")
    private Boolean isRead;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}