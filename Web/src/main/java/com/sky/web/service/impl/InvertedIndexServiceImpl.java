package com.sky.web.service.impl;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;
import com.sky.pojo.entity.InvertedIndex;
import com.sky.pojo.entity.Video;
import com.sky.pojo.mapper.InvertedIndexMapper;
import com.sky.web.service.InvertedIndexService;
import jakarta.annotation.Resource;
import org.roaringbitmap.RoaringBitmap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.*;

@Service
public class InvertedIndexServiceImpl implements InvertedIndexService {

    @Resource
    private InvertedIndexMapper invertedIndexMapper;

    private static final JiebaSegmenter segmenter = new JiebaSegmenter();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchInsertOrUpdateInvertedIndex(List<Video> videos) throws IOException {
        Map<String, InvertedIndex> invertedIndexMap = new HashMap<>();

        for (Video video : videos) {
            int docId = video.getId().intValue();
            String content = video.getTitle() + " " + video.getDescription();
            Map<String, Integer> wordCount = new HashMap<>();
            for (SegToken token : segmenter.process(content, JiebaSegmenter.SegMode.INDEX)) {
                String word = token.word.trim();
                if (!word.isEmpty()) {
                    wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
                }
            }

            for (Map.Entry<String, Integer> entry : wordCount.entrySet()) {
                String word = entry.getKey();
                int termFreq = entry.getValue();
                InvertedIndex invertedIndex = invertedIndexMap.getOrDefault(word, new InvertedIndex());
                invertedIndex.setWord(word);

                RoaringBitmap bitmap = invertedIndex.getBitmap() == null ? new RoaringBitmap() : deserialize(invertedIndex.getBitmap());
                bitmap.add(docId);
                invertedIndex.setBitmap(serialize(bitmap));

                List<Double> tfIdfList = invertedIndex.getTfIdf() == null ? new ArrayList<>() : deserializeTfIdfList(invertedIndex.getTfIdf());
                tfIdfList.add(computeTfIdf(termFreq, videos.size(), 1));
                invertedIndex.setTfIdf(serializeTfIdfList(tfIdfList));

                invertedIndex.setDocFreq(invertedIndex.getDocFreq() + 1);
                invertedIndexMap.put(word, invertedIndex);
            }
        }

        List<InvertedIndex> invertedIndexList = new ArrayList<>(invertedIndexMap.values());

        // 处理更新逻辑
        for (InvertedIndex invertedIndex : invertedIndexList) {
            InvertedIndex existingIndex = invertedIndexMapper.selectByWord(invertedIndex.getWord());
            if (existingIndex != null) {
                RoaringBitmap existingBitmap = deserialize(existingIndex.getBitmap());
                RoaringBitmap newBitmap = deserialize(invertedIndex.getBitmap());
                existingBitmap.or(newBitmap);
                invertedIndex.setBitmap(serialize(existingBitmap));

                List<Double> existingTfIdfList = deserializeTfIdfList(existingIndex.getTfIdf());
                List<Double> newTfIdfList = deserializeTfIdfList(invertedIndex.getTfIdf());
                existingTfIdfList.addAll(newTfIdfList);
                invertedIndex.setTfIdf(serializeTfIdfList(existingTfIdfList));

                invertedIndex.setDocFreq(existingIndex.getDocFreq() + invertedIndex.getDocFreq());
            }
        }

        invertedIndexMapper.batchInsertOrUpdate(invertedIndexList);
    }

    @Override
    public Map<Integer, Double> getDocScores(String keyword) throws IOException {
        Set<String> tokens = new HashSet<>();
        for (SegToken token : segmenter.process(keyword, JiebaSegmenter.SegMode.INDEX)) {
            tokens.add(token.word);
        }

        Map<Integer, Double> docScores = new HashMap<>();
        for (String token : tokens) {
            InvertedIndex invertedIndex = invertedIndexMapper.selectByWord(token);
            if (invertedIndex != null) {
                RoaringBitmap bitmap = deserialize(invertedIndex.getBitmap());
                List<Double> tfIdfList = deserializeTfIdfList(invertedIndex.getTfIdf());
                double tfIdf = tfIdfList.stream().mapToDouble(Double::doubleValue).sum();
                for (int docId : bitmap) {
                    docScores.put(docId, docScores.getOrDefault(docId, 0.0) + tfIdf);
                }
            } else {
                return null;
            }
        }
        return docScores;
    }

    private static double computeTfIdf(int termFreq, int totalDocs, int docFreq) {
        double tf = 1 + Math.log(termFreq);
        double idf = Math.log((double) totalDocs / (1 + docFreq));
        return tf * idf;
    }

    private static byte[] serialize(RoaringBitmap bitmap) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            bitmap.serialize(dos);
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    private static RoaringBitmap deserialize(byte[] bytes) {
        RoaringBitmap bitmap = new RoaringBitmap();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             DataInputStream dis = new DataInputStream(bais)) {
            bitmap.deserialize(dis);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    private static byte[] serializeTfIdfList(List<Double> tfIdfList) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        for (double tfIdf : tfIdfList) {
            dos.writeDouble(tfIdf);
        }
        return baos.toByteArray();
    }

    private static List<Double> deserializeTfIdfList(byte[] bytes) throws IOException {
        List<Double> tfIdfList = new ArrayList<>();
        if (bytes != null) {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
            while (dis.available() > 0) {
                tfIdfList.add(dis.readDouble());
            }
        }
        return tfIdfList;
    }
}