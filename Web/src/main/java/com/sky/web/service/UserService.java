package com.sky.web.service;

import com.sky.common.utils.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sky.pojo.dto.*;
import org.springframework.stereotype.Service;

@Service
public interface UserService {

    Result getUserInfo(Long userId);

    Result usernameLogin(UserNameLoginParam userNameLoginParam);

    Result emailLogin(EmailLoginParam emailLoginParam);

    Result sign(SignParam signParam);

    Result getCode(String email, String type) throws JsonProcessingException, InterruptedException;

    Result changePassword(ChangePasswordParam changePasswordParam);

    Result forgetPassword(ForgetPassWordParam forgetPassWordParam);

    Result quit(Long userId);

    Result refreshToken(Long userId);

    Result updateUserInfo(Long userId, UpdateUserInfoParam updateUserInfoParam);

    Result signIn(Long aLong);
}
