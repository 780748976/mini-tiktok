package com.sky.admin.service;

import com.sky.common.utils.Result;
import com.sky.pojo.dto.AuditVideoParam;

public interface PendingVideoService {

    Result auditVideo(AuditVideoParam auditVideoParam, Long adminId);

    Result getPendingVideos(Integer page, Integer pageSize);
}