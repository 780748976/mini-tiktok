package com.sky.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SseVo {

    @Schema(description = "内容")
    private String content;

    @Schema(description = "消息类型 system通知 comment评论通知 like点赞通知 mention@通知 message消息")
    private String type;
}
