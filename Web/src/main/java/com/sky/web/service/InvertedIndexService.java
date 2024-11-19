package com.sky.web.service;

import com.sky.pojo.entity.Video;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface InvertedIndexService {

    void batchInsertOrUpdateInvertedIndex(List<Video> videos) throws IOException;

    Map<Integer, Double> getDocScores(String keyword) throws IOException;
}