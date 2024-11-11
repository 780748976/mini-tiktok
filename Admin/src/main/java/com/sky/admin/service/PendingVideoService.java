package com.sky.admin.service;

import com.sky.common.utils.Result;
import com.sky.pojo.dto.AuditVideoParam;

import java.util.concurrent.ExecutionException;

public interface PendingVideoService {

    Result auditVideo(AuditVideoParam auditVideoParam, Long adminId) throws ExecutionException, InterruptedException;

    Result getPendingVideos(Integer page, Integer pageSize);
}