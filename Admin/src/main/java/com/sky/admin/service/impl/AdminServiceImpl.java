package com.sky.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.admin.service.AdminService;
import com.sky.common.utils.*;
import com.sky.pojo.constant.WebRedisConstants;
import com.sky.pojo.dto.*;
import com.sky.pojo.entity.Admin;
import com.sky.pojo.entity.InviteRecord;
import com.sky.pojo.mapper.AdminMapper;
import com.sky.pojo.mapper.InviteRecordMapper;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Service
public class AdminServiceImpl implements AdminService {

    @Resource
    AdminMapper adminMapper;
    @Resource
    JwtUtil jwtUtil;
    @Resource
    BCryptPasswordEncoder bCryptPasswordEncoder;
    @Resource
    AliyunOss aliyunOss;
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Resource
    RedissonClient redissonClient;
    @Resource
    MailUtil mailUtil;
    @Resource
    InviteRecordMapper inviteRecordMapper;

    @Override
    public Result usernameLogin(UserNameLoginParam userNameLoginParam) {
        LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Admin::getUsername, userNameLoginParam.getUsername())
                .select(Admin::getId, Admin::getPassword);
        Admin admin = adminMapper.selectOne(queryWrapper);
        if(admin == null){
            return Result.failed("用户名或密码错误");
        }
        if (!bCryptPasswordEncoder.matches(userNameLoginParam.getPassword(), admin.getPassword())) {
            return Result.failed("用户名或密码错误");
        }
        return getTokenMap(admin.getId(), "登录成功");
    }

    //获取accessToken和refreshToken
    public Result getTokenMap(Long userId, String ResultMessage) {
        HashMap<String, String> tokenMap = new HashMap<>();
        tokenMap.put("accessToken", jwtUtil.createJWT(String.valueOf(userId)));
        tokenMap.put("refreshToken", jwtUtil.createJWT(String.valueOf(userId), jwtUtil.jwtTtl * 7));
        return Result.success(tokenMap, ResultMessage);
    }

    @Override
    public Result emailLogin(EmailLoginParam emailLoginParam) {
        LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Admin::getEmail, emailLoginParam.getEmail())
                .select(Admin::getId, Admin::getPassword);
        Admin admin = adminMapper.selectOne(queryWrapper);
        if(admin == null){
            return Result.failed("邮箱或密码错误");
        }
        if (!bCryptPasswordEncoder.matches(emailLoginParam.getPassword(), admin.getPassword())) {
            return Result.failed("邮箱或密码错误");
        }
        return getTokenMap(admin.getId(), "登录成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result sign(AdminSignParam adminSignParam) {
        String inviter = stringRedisTemplate.opsForValue()
                .get(WebRedisConstants.ADMIN_INVITE_CODE_KEY + adminSignParam.getInviteCode());
        if (inviter == null) {
            return Result.failed("邀请码无效");
        }
        LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Admin::getUsername, adminSignParam.getUsername());
        Admin admin = adminMapper.selectOne(queryWrapper);
        if(admin != null){
            return Result.failed("该账号已注册");
        }
        queryWrapper.clear();
        queryWrapper.eq(Admin::getEmail, adminSignParam.getEmail());
        admin = adminMapper.selectOne(queryWrapper);
        if(admin != null){
            return Result.failed("该邮箱已注册");
        }
        if (!checkCode(adminSignParam.getEmail(), adminSignParam.getCode(), "sign")) {
            return Result.failed("验证码错误");
        }
        admin = new Admin();
        admin.setUsername(adminSignParam.getUsername())
                .setPassword(bCryptPasswordEncoder.encode(adminSignParam.getPassword()))
                .setEmail(adminSignParam.getEmail());
        adminMapper.insert(admin);
        InviteRecord inviteRecord = new InviteRecord();
        inviteRecord.setInviterId(Long.valueOf(inviter))
                .setInviteeId(admin.getId())
                .setCreateTime(LocalDateTime.now());
        inviteRecordMapper.insert(inviteRecord);
        stringRedisTemplate.delete(WebRedisConstants.ADMIN_INVITE_CODE_KEY + adminSignParam.getInviteCode());
        return Result.success("注册成功");
    }

    @Override
    public Result getCode(String email, String type) throws InterruptedException {
        RLock rlock = redissonClient.getLock(WebRedisConstants.EMAIL_CODE_LOCK + email);
        boolean getLock = rlock.tryLock(0,60, TimeUnit.SECONDS);
        if(!getLock){
            return Result.failed("请勿频繁发送验证码");
        }
        String MAIL_SUBJECT = "验证码";
        int code = CodeGen.generatedCode(6);
        String MAIL_TEXT = "您的验证码为："+code+"，请在5分钟内完成验证。";
        mailUtil.send(email, MAIL_SUBJECT,MAIL_TEXT);
        stringRedisTemplate.opsForValue().set(WebRedisConstants.EMAIL_CODE_KEY + type + email,
                String.valueOf(code), 300L, TimeUnit.SECONDS);
        return Result.success("验证码已发送");
    }

    @Override
    public Result changePassword(ChangePasswordParam changePasswordParam) {
        LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Admin::getUsername, changePasswordParam.getUsername())
                .select(Admin::getId, Admin::getPassword)
                .last("limit 1");
        Admin admin = adminMapper.selectOne(queryWrapper);
        if(admin == null){
            return Result.failed("账号不存在");
        }
        if (!bCryptPasswordEncoder.matches(changePasswordParam.getPassword(), admin.getPassword())) {
            return Result.failed("原密码错误");
        }
        if (changePasswordParam.getNewPassword().equals(changePasswordParam.getPassword())) {
            return Result.failed("新密码不能与原密码相同");
        }
        admin.setPassword(bCryptPasswordEncoder.encode(changePasswordParam.getNewPassword()));
        adminMapper.updateById(admin);
        return Result.success("修改成功");
    }

    @Override
    public Result forgetPassword(ForgetPassWordParam forgetPassWordParam) {
        LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Admin::getUsername, forgetPassWordParam.getUsername());
        Admin admin = adminMapper.selectOne(queryWrapper);
        if(admin == null){
            return Result.failed("账号不存在");
        }
        if (!checkCode(admin.getEmail(), forgetPassWordParam.getCode(), "forget")) {
            return Result.failed("验证码错误");
        }
        admin.setPassword(bCryptPasswordEncoder.encode(forgetPassWordParam.getNewPassword()));
        adminMapper.updateById(admin);
        return Result.success("修改成功");
    }

    @Override
    public Result quit(Long userId) {
        stringRedisTemplate.opsForValue().set(WebRedisConstants.USER_LOGIN_BLACKLIST_KEY + userId,
                String.valueOf(System.currentTimeMillis()), jwtUtil.jwtTtl, TimeUnit.SECONDS);
        return Result.success("退出成功");
    }

    @Override
    public Result refreshToken(Long userId) {
        return getTokenMap(userId, null);
    }

    @Override
    public Result publishInviteCode(Long userId) {
        stringRedisTemplate.opsForValue().set(WebRedisConstants.ADMIN_INVITE_CODE_KEY + UUID.randomUUID(),
                String.valueOf(userId), 3, TimeUnit.DAYS);
        return Result.success("创建成功，邀请码有效期为3天");
    }

    public boolean checkCode(String email, String code, String type) {;
        //判断验证码是否正确
        String realCode =  stringRedisTemplate.opsForValue().get(WebRedisConstants.EMAIL_CODE_KEY + type + email);
        if (realCode == null)
            return false;
        return Objects.equals(realCode, code);
    }
}
