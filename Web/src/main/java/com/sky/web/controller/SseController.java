package com.sky.web.controller;

import com.sky.common.utils.SseEmitterUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@Tag(name = "Sse", description = "服务器推送接口")
@RequestMapping("/admin/sse")
public class SseController {

    @GetMapping("/connect")
    @Operation(summary = "连接服务器推送")
    public SseEmitter connect() {
        Long userId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        return SseEmitterUtil.connect(String.valueOf(userId));
    }

}
