package com.sky.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SseVo {

    @Schema(description = "内容")
    private String content;

    @Schema(description = "消息类型 notice通知 message消息")
    private String type;
}
