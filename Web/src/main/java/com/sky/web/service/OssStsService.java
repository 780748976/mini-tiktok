package com.sky.web.service;

import com.aliyuncs.exceptions.ClientException;
import com.sky.common.utils.Result;
import com.sky.pojo.dto.GetOssStsParam;
import org.springframework.stereotype.Service;

@Service
public interface OssStsService {
    Result getOssStsImage(Long userId, GetOssStsParam getOssStsParam) throws ClientException;

    Result getOssStsVideo(Long userId, String suffix) throws ClientException;
}
