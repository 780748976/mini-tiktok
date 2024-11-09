package com.sky.admin.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sky.admin.service.AdminService;
import com.sky.common.utils.Result;
import com.sky.pojo.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 系统管理-用户基础信息表 前端控制器
 * </p>
 *
 * @author lin
 * @since 2024-07-24 05:19:39
 */
@RestController
@RequestMapping("/admin")
@Tag(name = "管理员接口")
@Validated
public class AdminController {

    @Resource
    AdminService adminService;

    @Operation(summary = "发布邀请码")
    @GetMapping("/publish_invite_code")
    public Result publishInviteCode() {
        Long userId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        return adminService.publishInviteCode(userId);
    }

    @PostMapping("/username_login")
    @Operation(summary = "通过用户名登录")
    public Result login(@RequestBody @Validated UserNameLoginParam userNameLoginParam) {
        return adminService.usernameLogin(userNameLoginParam);
    }

    @PostMapping("/email_login")
    @Operation(summary = "通过邮箱登录")
    public Result login(@RequestBody @Validated EmailLoginParam emailLoginParam) {
        return adminService.emailLogin(emailLoginParam);
    }


    @PostMapping("/sign")
    @Operation(summary = "用户注册")
    public Result sign(@RequestBody @Validated AdminSignParam adminSignParam) {
        return adminService.sign(adminSignParam);
    }

    @GetMapping("/get_code_by_email")
    @Operation(summary = "获取邮箱验证码 sign注册 forget忘记密码")
    public Result getCode(@RequestParam @Email(message = "邮箱格式错误") String email
    , @RequestParam @NotBlank String type) throws JsonProcessingException, InterruptedException {
        return adminService.getCode(email, type);
    }

    @Operation(summary = "修改密码")
    @PutMapping("/change_password")
    public Result changePassword(@Validated @RequestBody ChangePasswordParam changePasswordParam) {
        return adminService.changePassword(changePasswordParam);
    }

    @Operation(summary = "忘记密码")
    @PutMapping("/forget_password")
    public Result forgetPassword(@Validated @RequestBody ForgetPassWordParam forgetPassWordParam) {
        return adminService.forgetPassword(forgetPassWordParam);
    }


    @GetMapping("quit")
    @Operation(summary = "退出登录")
    public Result quit() {
        Long userId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        return adminService.quit(userId);
    }

    @GetMapping("/refresh_token")
    @Operation(summary = "刷新token")
    public Result refreshToken() {
        Long userId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        return adminService.refreshToken(userId);
    }
}
