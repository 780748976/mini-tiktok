package com.sky.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.admin.service.TagService;
import com.sky.common.utils.Result;
import com.sky.pojo.dto.TagParam;
import com.sky.pojo.entity.Tag;
import com.sky.pojo.mapper.TagMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TagServiceImpl implements TagService {

    @Resource
    private TagMapper tagMapper;

    @Override
    public Result createTag(TagParam tagParam) {
        //判断标签是否带空格
        if (tagParam.getName().contains(" ")) {
            return Result.failed("标签名不能包含空格");
        }
        if (tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, tagParam.getName())) != null) {
            return Result.failed("标签已存在");
        }
        Tag tag = new Tag();
        BeanUtils.copyProperties(tagParam, tag);
        tagMapper.insert(tag);
        return Result.success(tag);
    }

    @Override
    public Result getTagById(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            return Result.failed("标签不存在");
        }
        TagParam tagParam = new TagParam();
        BeanUtils.copyProperties(tag, tagParam);
        return Result.success(tagParam);
    }

    @Override
    public Result getTagsByIds(List<Long> ids) {
        List<Tag> tags = tagMapper.selectBatchIds(ids);
        List<TagParam> tagParams = tags.stream().map(tag -> {
            TagParam dto = new TagParam();
            BeanUtils.copyProperties(tag, dto);
            return dto;
        }).collect(Collectors.toList());
        return Result.success(tagParams);
    }

    @Override
    public Result updateTag(Long id, TagParam tagParam) {
        //判断标签是否带空格
        if (tagParam.getName().contains(" ")) {
            return Result.failed("标签名不能包含空格");
        }
        if (tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, tagParam.getName())) != null) {
            return Result.failed("标签已存在");
        }
        Tag existingTag = tagMapper.selectById(id);
        if (existingTag == null) {
            return Result.failed("标签不存在");
        }
        BeanUtils.copyProperties(tagParam, existingTag);
        tagMapper.updateById(existingTag);
        return Result.success(existingTag);
    }

    @Override
    public Result deleteTag(Long id) {
        int result = tagMapper.deleteById(id);
        if (result > 0) {
            return Result.success("标签删除成功");
        }
        return Result.failed("删除失败");
    }

    @Override
    public Result getAllTags(int page, int size, String search) {
        Page<Tag> tagPage = new Page<>(page, size);
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        if (search != null && !search.isEmpty()) {
            queryWrapper.like(Tag::getName, search);
        }
        tagMapper.selectPage(tagPage, queryWrapper);
        List<TagParam> tagParams = tagPage.getRecords().stream().map(tag -> {
            TagParam dto = new TagParam();
            BeanUtils.copyProperties(tag, dto);
            return dto;
        }).collect(Collectors.toList());
        return Result.success(tagParams);
    }

    @Override
    public Result searchTags(String keyword, int page, int size) {
        Page<Tag> tagPage = new Page<>(page, size);
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Tag::getName, keyword);
        tagMapper.selectPage(tagPage, queryWrapper);
        List<TagParam> tagParams = tagPage.getRecords().stream().map(tag -> {
            TagParam dto = new TagParam();
            BeanUtils.copyProperties(tag, dto);
            return dto;
        }).collect(Collectors.toList());
        return Result.success(tagParams);
    }
}