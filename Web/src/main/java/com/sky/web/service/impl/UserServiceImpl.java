package com.sky.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.common.utils.*;
import com.sky.pojo.constant.WebOssConstants;
import com.sky.pojo.constant.WebRedisConstants;
import com.sky.pojo.dto.*;
import com.sky.pojo.entity.User;
import com.sky.pojo.entity.UserInfo;
import com.sky.pojo.mapper.UserInfoMapper;
import com.sky.pojo.mapper.UserMapper;
import com.sky.web.service.UserService;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


@Service
public class UserServiceImpl implements UserService {

    @Resource
    UserMapper userMapper;
    @Resource
    UserInfoMapper userInfoMapper;
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

    @Override
    public Result getUserInfo(Long userId) {
        UserInfo userInfo = userInfoMapper.selectById(userId);
        return Result.success(userInfo);
    }

    @Override
    public Result usernameLogin(UserNameLoginParam userNameLoginParam) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, userNameLoginParam.getUsername())
                .select(User::getId, User::getPassword);
        User user = userMapper.selectOne(queryWrapper);
        if(user == null){
            return Result.failed("用户名或密码错误");
        }
        if (!bCryptPasswordEncoder.matches(userNameLoginParam.getPassword(), user.getPassword())) {
            return Result.failed("用户名或密码错误");
        }
        return getTokenMap(user.getId(), "登录成功");
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
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getEmail, emailLoginParam.getEmail())
                .select(User::getId, User::getPassword);
        User bUser = userMapper.selectOne(queryWrapper);
        if(bUser == null){
            return Result.failed("邮箱或密码错误");
        }
        if (!bCryptPasswordEncoder.matches(emailLoginParam.getPassword(), bUser.getPassword())) {
            return Result.failed("邮箱或密码错误");
        }
        return getTokenMap(bUser.getId(), "登录成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result sign(SignParam signParam) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, signParam.getUsername());
        User bUser = userMapper.selectOne(queryWrapper);
        if(bUser != null){
            return Result.failed("该账号已注册");
        }
        queryWrapper.clear();
        queryWrapper.eq(User::getEmail, signParam.getEmail());
        bUser = userMapper.selectOne(queryWrapper);
        if(bUser != null){
            return Result.failed("该邮箱已注册");
        }
        if (!checkCode(signParam.getEmail(), signParam.getCode(), "sign")) {
            return Result.failed("验证码错误");
        }
        User user = new User();
        user.setUsername(signParam.getUsername())
                .setPassword(bCryptPasswordEncoder.encode(signParam.getPassword()))
                .setEmail(signParam.getEmail());
        userMapper.insert(user);
        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.getId());
        userInfo.setNickname("用户" + user.getId());
        userInfo.setAvatar(WebOssConstants.DEFAULT_USER_AVATAR);
        userInfoMapper.insert(userInfo);
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
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, changePasswordParam.getUsername())
                .select(User::getId, User::getPassword)
                .last("limit 1");
        User user = userMapper.selectOne(queryWrapper);
        if(user == null){
            return Result.failed("账号不存在");
        }
        if (!bCryptPasswordEncoder.matches(changePasswordParam.getPassword(), user.getPassword())) {
            return Result.failed("原密码错误");
        }
        if (changePasswordParam.getNewPassword().equals(changePasswordParam.getPassword())) {
            return Result.failed("新密码不能与原密码相同");
        }
        user.setPassword(bCryptPasswordEncoder.encode(changePasswordParam.getNewPassword()));
        userMapper.updateById(user);
        return Result.success("修改成功");
    }

    @Override
    public Result forgetPassword(ForgetPassWordParam forgetPassWordParam) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, forgetPassWordParam.getUsername());
        User user = userMapper.selectOne(queryWrapper);
        if(user == null){
            return Result.failed("账号不存在");
        }
        if (!checkCode(user.getEmail(), forgetPassWordParam.getCode(), "forget")) {
            return Result.failed("验证码错误");
        }
        user.setPassword(bCryptPasswordEncoder.encode(forgetPassWordParam.getNewPassword()));
        userMapper.updateById(user);
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
    public Result updateUserInfo(Long userId, UpdateUserInfoParam updateUserInfoParam) {
        if (updateUserInfoParam.getSex() != null && updateUserInfoParam.getSex() != 0 && updateUserInfoParam.getSex() != 1) {
            return Result.failed("性别参数错误");
        }
        if (updateUserInfoParam.getBjCover() != null && aliyunOss.findFile(updateUserInfoParam.getBjCover())){
            return Result.failed("个人中心背景图未上传");
        }
        if (updateUserInfoParam.getAvatar() != null && aliyunOss.findFile(updateUserInfoParam.getAvatar())){
            return Result.failed("头像未上传");
        }
        if (updateUserInfoParam.getNickname() != null && updateUserInfoParam.getNickname().length() > 25){
            return Result.failed("昵称过长");
        }
        if (updateUserInfoParam.getIntro() != null && updateUserInfoParam.getIntro().length() > 255){
            return Result.failed("简介过长");
        }
        UserInfo userInfo = new UserInfo();
        userInfo.setId(userId)
                .setNickname(updateUserInfoParam.getNickname())
                .setAvatar(updateUserInfoParam.getAvatar())
                .setIntro(updateUserInfoParam.getIntro())
                .setSex(updateUserInfoParam.getSex())
                .setBjCover(updateUserInfoParam.getBjCover());
        userInfoMapper.updateById(userInfo);
        return Result.success("更新成功");
    }

    public boolean checkCode(String email, String code ,String type) {;
        //判断验证码是否正确
        /*log.info(WebRedisConstants.EMAIL_CODE_KEY + type + email);*/
        String realCode =  stringRedisTemplate.opsForValue().get(WebRedisConstants.EMAIL_CODE_KEY + type + email);
        if (realCode == null)
            return false;
        return Objects.equals(realCode, code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result signIn(Long userId) {
        boolean isSignIn = Boolean.TRUE.equals(
                stringRedisTemplate.opsForSet().isMember(WebRedisConstants.USER_TODAY_SIGN_IN_KEY, userId));
        if(isSignIn){
            return Result.failed("今日已签到");
        }
        //获取现在时间
        LocalDateTime end = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0)
                .withSecond(0).withNano(0);
        long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(),end);
        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfo::getId, userId)
                .select(UserInfo::getId, UserInfo::getCoins, UserInfo::getExperience)
                .last("limit 1");
        UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);
        userInfo.setCoins(userInfo.getCoins() + 1)
                .setExperience(userInfo.getExperience() + 10);
        userInfoMapper.updateById(userInfo);
        stringRedisTemplate.opsForSet().add(WebRedisConstants.USER_TODAY_SIGN_IN_KEY, String.valueOf(userId));
        return Result.success("签到成功");
    }
}
