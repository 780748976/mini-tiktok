package com.sky.web.service;

import com.sky.common.utils.Result;

public interface DynamicsService {

    Result getUserDynamics(Integer page, Integer size, Long userId);
}