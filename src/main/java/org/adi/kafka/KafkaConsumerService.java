package org.adi.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.adi.models.RedditPost;
import org.adi.service.MongoService;
import org.adi.service.OpenSearchService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class KafkaConsumerService {
    @Inject
    MongoService mongoService;

    @Inject
    OpenSearchService openSearchService;

    // ObjectMapper is a part of Jackson, used to map Java objects to JSON and vice versa
    private ObjectMapper objectMapper = new ObjectMapper();

    // this annotation indicates that this method listens to messages from the "reddit-posts" topic; whenever a message is received, it is passed to this method
    @Incoming("reddit-posts")
    public void consumePost(ConsumerRecord<String,String> record){
        try{
            // we are converting the JSON string to RedditPost object using ObjectMapper
            RedditPost post = objectMapper.readValue(record.value(), RedditPost.class);

            // save to MongoDB
            Boolean exists = mongoService.savePostsToDatabase(post);

            // Index in OpenSearch
            if(!exists)
                openSearchService.indexPost(post);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
