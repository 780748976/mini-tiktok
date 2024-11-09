package com.sky.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "审核视频参数")
public class AuditVideoParam {
    @Schema(description = "视频ID")
    @NotNull(message = "视频ID不可为空")
    private Long videoId;

    @Schema(description = "审核状态")
    @NotBlank(message = "审核状态不可为空")
    private String auditStatus;

    @Schema(description = "审核意见")
    private String auditComment;
}