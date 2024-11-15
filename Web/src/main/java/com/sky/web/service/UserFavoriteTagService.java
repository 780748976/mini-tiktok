package com.sky.web.service;

public interface UserFavoriteTagService {

    void changeProbability(Long videoId, Long userId, Integer Type);
}