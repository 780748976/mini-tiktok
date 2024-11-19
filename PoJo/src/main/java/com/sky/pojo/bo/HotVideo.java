package com.sky.pojo.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "热点视频")
public class HotVideo {

    @Schema(description = "视频id")
    private Long id;

    @Schema(description = "热度")
    private Double hot;

}