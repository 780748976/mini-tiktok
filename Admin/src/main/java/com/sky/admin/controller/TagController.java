package com.sky.admin.controller;

import com.sky.common.utils.Result;
import com.sky.admin.service.TagService;
import com.sky.pojo.dto.TagParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/tag")
@Tag(name = "Tag", description = "标签管理接口")
@Validated
public class TagController {

    @Resource
    private TagService tagService;

    @PostMapping
    @Operation(summary = "创建新标签")
    public Result createTag(@Valid @RequestBody TagParam tagParam) {
        return tagService.createTag(tagParam);
    }

    @GetMapping("/batch")
    @Operation(summary = "根据IDs批量获取标签")
    public Result getTagsByIds(@RequestParam @NotEmpty(message = "ID列表不能为空") List<Long> ids) {
        return tagService.getTagsByIds(ids);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新标签")
    public Result updateTag(@PathVariable @NotNull Long id, @Valid @RequestBody TagParam tagParam) {
        return tagService.updateTag(id, tagParam);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除标签")
    public Result deleteTag(@PathVariable @NotNull Long id) {
        return tagService.deleteTag(id);
    }

    @GetMapping
    @Operation(summary = "获取标签列表（带分页和查询）")
    public Result getAllTags(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search
    ) {
        return tagService.getAllTags(page, size, search);
    }

    @GetMapping("/search")
    @Operation(summary = "查询标签")
    public Result searchTags(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return tagService.searchTags(keyword, page, size);
    }
}