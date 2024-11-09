import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetResponse;
import com.sky.pojo.entity.Video;
import com.sky.web.WebApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest(
        classes = {WebApplication.class}
)
public class TestEsService {

    @Autowired
    ElasticsearchClient esClient;

    @Test
    public void testEsClient() throws IOException {
        GetResponse<Video> video = esClient.get(g -> g.index("video").id("1"), Video.class);
        // 输出查询结果
        System.out.println(video.source());
    }
}
