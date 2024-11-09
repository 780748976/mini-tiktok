package com.sky.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sky.common.utils.Result;
import com.sky.pojo.dto.*;
import org.springframework.stereotype.Service;

@Service
public interface AdminService {

    Result usernameLogin(UserNameLoginParam userNameLoginParam);

    Result emailLogin(EmailLoginParam emailLoginParam);

    Result sign(AdminSignParam adminSignParam);

    Result getCode(String email, String type) throws JsonProcessingException, InterruptedException;

    Result changePassword(ChangePasswordParam changePasswordParam);

    Result forgetPassword(ForgetPassWordParam forgetPassWordParam);

    Result quit(Long userId);

    Result refreshToken(Long userId);

    Result publishInviteCode(Long userId);
}
