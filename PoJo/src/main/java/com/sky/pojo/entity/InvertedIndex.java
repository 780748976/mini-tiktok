package com.sky.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("inverted_index")
@Schema(name = "InvertedIndex", description = "倒排索引表")
public class InvertedIndex {

    @TableId
    @Schema(description = "分词主键ID")
    private String word;

    @Schema(description = "索引位图")
    private byte[] bitmap;

    @Schema(description = "TF-IDF值")
    private byte[] tfIdf;

    @Schema(description = "分词在文档中的频率")
    private int docFreq;
}