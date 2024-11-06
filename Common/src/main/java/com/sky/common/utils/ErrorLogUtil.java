package com.sky.common.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ErrorLogUtil {
    //记录log
    public static void save(Exception ex){
        StringBuilder message = new StringBuilder(ex.getMessage());
        for (StackTraceElement stackTraceElement : ex.getStackTrace()) {
            message.append("\n").append(stackTraceElement);
        }
        log.error(String.valueOf(message));
    }
}
