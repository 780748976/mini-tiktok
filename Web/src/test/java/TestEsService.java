import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.sky.pojo.entity.Video;
import com.sky.web.WebApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.time.LocalDateTime;

@SpringBootTest(
        classes = {WebApplication.class}
)
public class TestEsService {

    @Autowired
    ElasticsearchClient esClient;

    @Test
    public void testEsClient() throws IOException {
        Video video = new Video();
        video.setId(2L);
        video.setTitle("黑魂3 哈哈哈哈哈哈哈 魂5");
        video.setDescription("test");
        video.setUserId(1L);
        video.setCover("test");
        video.setUrl("test");
        video.setUploadTime(LocalDateTime.now());
        video.setViews(0L);
        esClient.index(i -> i.index("video").id("1").document(video));
    }

    @Test
    public void testEsClient2() throws IOException {
        String keyword = "黑魂";
        String finalSortType = "views";
        int page = 1;
        int size = 10;
        SearchResponse<Video> videos = esClient.search(i -> i
                .index("video")
                .query(q -> q
                        .match(m -> m
                                .field("title")
                                .query(keyword)
                        )
                )
                .sort(s -> s
                        .field(FieldSort.of(f -> f.field(finalSortType)
                                .order(SortOrder.Desc)))
                )
                .from((page - 1) * size)
                .size(size), Video.class);
        System.out.println(videos.hits().hits().get(0).source());
    }
}
