package com.sky.web.index;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.StringReader;

@Component
public class VideoIndex {

    @Resource
    ElasticsearchClient client;

    @PostConstruct
    public void init() throws IOException {
        String index = "video";
        //查询索引是否存在
        if (!client.indices().exists(i -> i.index(index)).value()) {
            String json = """ 
                {
                  "mappings": {
                    "properties": {
                      "id": {
                        "type": "long"
                      },
                      "title": {
                        "type": "text",
                        "analyzer": "ik_max_word",
                        "search_analyzer": "ik_smart"
                      },
                      "description": {
                        "type": "text",
                        "analyzer": "ik_max_word",
                        "search_analyzer": "ik_smart"
                      },
                      "cover": {
                        "type": "keyword"
                      },
                      "url": {
                        "type": "keyword"
                      },
                      "userId": {
                        "type": "long"
                      },
                      "uploadTime": {
                        "type": "date",
                        "format": "yyyy-MM-dd HH:mm:ss"
                      },
                      "views": {
                        "type": "long"
                      },
                      "likes": {
                        "type": "long"
                      },
                      "dislikes": {
                        "type": "long"
                      },
                      "status": {
                        "type": "integer"
                      }
                    }
                  }
                }
                """;
            //构建索引请求
            CreateIndexRequest request = CreateIndexRequest.of(i -> i
                    .index(index)
                    .withJson(new StringReader(json))
            );
            // 发送请求
            CreateIndexResponse response = client.indices().create(request);
            if (Boolean.FALSE.equals(response.acknowledged())) {
                throw new RuntimeException("创建索引失败");
            }
        }
    }
}
