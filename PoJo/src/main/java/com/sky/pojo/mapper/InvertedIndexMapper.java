package com.sky.pojo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.pojo.entity.InvertedIndex;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface InvertedIndexMapper extends BaseMapper<InvertedIndex> {

    @Select("SELECT * FROM inverted_index WHERE word = #{word}")
    InvertedIndex selectByWord(@Param("word") String word);

    @Update("<script>" +
            "INSERT INTO inverted_index (word, bitmap, tf_idf, doc_freq) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.word}, #{item.bitmap}, #{item.tfIdf}, #{item.docFreq})" +
            "</foreach>" +
            "ON DUPLICATE KEY UPDATE " +
            "bitmap = VALUES(bitmap), " +
            "tf_idf = VALUES(tf_idf), " +
            "doc_freq = VALUES(doc_freq)" +
            "</script>")
    void batchInsertOrUpdate(@Param("list") List<InvertedIndex> list);
}