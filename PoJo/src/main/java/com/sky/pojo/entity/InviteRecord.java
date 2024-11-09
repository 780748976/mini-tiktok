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
@TableName("invite_record")
@Schema(name = "InviteRecord", description = "邀请记录表")
public class InviteRecord {
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "邀请者ID")
    private Long inviterId;

    @Schema(description = "被邀请者ID")
    private Long inviteeId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}