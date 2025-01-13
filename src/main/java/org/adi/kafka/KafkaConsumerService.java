package org.adi.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.adi.models.RedditPost;
import org.adi.service.MongoService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class KafkaConsumerService {
    @Inject
    MongoService mongoService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Incoming("reddit-posts")
    public void consumePost(ConsumerRecord<String,String> record){
        try{
            String username = record.key();
            RedditPost post = objectMapper.readValue(record.value(), RedditPost.class);
            mongoService.savePostsToDatabase(post);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}