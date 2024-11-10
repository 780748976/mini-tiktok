import com.google.gson.Gson;
import com.sky.pojo.entity.Video;
import com.sky.web.WebApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.ExecutionException;

@SpringBootTest(
        classes = {WebApplication.class}
)
public class KafkaTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    Gson gson;

    @Test
    public void testKafka() throws ExecutionException, InterruptedException {
        Video video = new Video();
        video.setId(1L);
        video.setTitle("test");
        kafkaTemplate.send("video_audit", gson.toJson(video)).get();
    }
}
