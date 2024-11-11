package com.sky.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("UserFollow") // 修改表名为 UserFollow
public class UserFollow {
    @TableId(type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id;

    @Schema(description = "关注者的用户ID")
    private Long followerId;

    @Schema(description = "被关注者的用户ID")
    private Long followingId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}