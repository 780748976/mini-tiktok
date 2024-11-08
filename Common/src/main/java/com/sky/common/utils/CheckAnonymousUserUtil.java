package com.sky.common.utils;

public class CheckAnonymousUserUtil {
    public static boolean check(String token) {
        return token.equals("anonymousUser");
    }
}
