package com.sky.admin.service;

import com.sky.common.utils.Result;
import com.sky.pojo.dto.TagParam;

import java.util.List;

public interface TagService {
    Result createTag(TagParam tagParam);
    Result getTagById(Long id);
    Result getTagsByIds(List<Long> ids);
    Result updateTag(Long id, TagParam tagParam);
    Result deleteTag(Long id);
    Result getAllTags(int page, int size, String search);
    Result searchTags(String keyword, int page, int size);
}