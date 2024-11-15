package com.sky.web.controller;

import com.sky.common.utils.Result;
import com.sky.web.service.DynamicsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dynamics")
@Tag(name = "Dynamics", description = "用户动态接口")
@Validated
public class DynamicsController {

    @Resource
    private DynamicsService dynamicsService;

    @GetMapping("/get_user_dynamics_page")
    @Operation(summary = "分页查看用户动态")
    public Result getUserDynamics(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer size) {
        Long userId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        return dynamicsService.getUserDynamics(page, size,userId);
    }
}