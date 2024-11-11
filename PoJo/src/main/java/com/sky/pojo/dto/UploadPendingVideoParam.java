package com.sky.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "上传待审核视频参数")
public class UploadPendingVideoParam {

    @Schema(description = "视频标题")
    @NotBlank(message = "视频标题不可为空")
    private String title;

    @Schema(description = "视频封面")
    private String cover;

    @Schema(description = "视频描述")
    private String description;

    @Schema(description = "视频链接")
    @NotBlank(message = "视频链接不可为空")
    private String url;

    @Schema(description = "标签")
    private List<String> tags;
}
