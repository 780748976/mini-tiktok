package com.sky.common.utils;

public class CheckAnonymousUserUtil {
    public static Long check(String token) {
        if (token.equals("anonymousUser")){
            return null;
        }
        return Long.parseLong(token);
    }
}
