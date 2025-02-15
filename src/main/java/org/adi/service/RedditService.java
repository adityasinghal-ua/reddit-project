package org.adi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.adi.config.Constants;
import org.adi.controller.client.RedditAuthClient;
import org.adi.controller.client.RedditUserClient;
import org.adi.kafka.KafkaProducerService;
import org.adi.models.RedditPost;
import org.adi.models.RedditResponseWrapper;
import org.bson.Document;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@ApplicationScoped
public class RedditService {

    @Inject
    @RestClient
    RedditAuthClient authClient;  // calls reddit.com/api/v1/access_token

    @Inject
    @RestClient
    RedditUserClient userClient;  // calls oauth.reddit.com/user/{username}/submitted

    @Inject
    MongoService mongoService;

    @Inject
    KafkaProducerService kafkaProducer;

    @Inject
    RedisService redisService;


//       1) Check Redis
//       2) If missing, check DB
//       3) If still missing, fetch from Reddit, store in DB (via Kafka), and store in Redis

    public CompletableFuture<List<RedditPost>> getUserPosts(
            String username,
            String clientId,
            String clientSecret,
            Integer limit,
            Integer offset
    ) {
        //  Check Redis cache
        String cacheKey = redisService.buildRedisKey(username, limit, offset);
        List<RedditPost> fromRedis = redisService.getPostsFromCache(cacheKey);
        if (fromRedis != null && !fromRedis.isEmpty()) {
            System.out.println("Data retrieved from Redis cache");
            return CompletableFuture.completedFuture(fromRedis);
        }

        // If not in Redis, check Mongo
        return CompletableFuture.supplyAsync(() -> {
            List<RedditPost> fromMongo = mongoService.getPostsFromDatabase(username, limit, offset);
            return fromMongo;
        }).thenCompose(posts -> {
            if (posts != null && !posts.isEmpty() && posts.size() == limit) {
                System.out.println("Data retrieved from MongoDB");

                // Cache these posts in Redis for next time
                redisService.setPostsToCache(cacheKey, posts, 600);

                return CompletableFuture.completedFuture(posts);
            } else {

                // If not in Mongo, fetch from Reddit
                return getAccessToken(clientId, clientSecret)
                        .thenCompose(accessToken -> fetchPostsFromReddit(username, accessToken, limit, offset))
                        .thenApply(fetchedPosts -> {
                            System.out.println("Data retrieved from Reddit API");

                            // Publish new posts to Kafka -> send to MongoDB and OpenSearch
                            fetchedPosts.forEach(post ->
                                    kafkaProducer.sendMessage(Constants.REDDIT_TOPIC, username, serializePost(post))
                            );

                            // Cache in Redis
                            redisService.setPostsToCache(cacheKey, fetchedPosts, 3600);

                            return fetchedPosts;
                        });
            }
        });
    }


    private CompletableFuture<String> getAccessToken(String clientId, String clientSecret) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String basicAuth = "Basic " + Base64.getEncoder()
                        .encodeToString((clientId + ":" + clientSecret).getBytes());
                String tokenResponse = authClient.getAccessToken(basicAuth, "client_credentials");
                JsonNode jsonNode = new ObjectMapper().readTree(tokenResponse);
                return jsonNode.get("access_token").asText();
            } catch (Exception e) {
                throw new RuntimeException("Failed to get access token", e);
            }
        });
    }


    private CompletableFuture<List<RedditPost>> fetchPostsFromReddit(String username, String accessToken, Integer limit, Integer offset) {
        return CompletableFuture.supplyAsync(() -> {
            List<RedditPost> fetchedPosts = new ArrayList<>();
            String after = null;

            while ((after != null || fetchedPosts.isEmpty()) && fetchedPosts.size() < limit + offset) {
                RedditResponseWrapper responseWrapper =
                        userClient.getUserPosts("Bearer " + accessToken, username, 100, after);

                if (responseWrapper == null || responseWrapper.getData() == null
                        || responseWrapper.getData().getChildren().isEmpty()) {
                    throw new NotFoundException("No posts found for user: " + username);
                }

                fetchedPosts.addAll(responseWrapper.getData().getChildren().stream()
                        .map(RedditResponseWrapper.Child::getData)
                        .collect(Collectors.toList()));

                after = responseWrapper.getData().getAfter();
            }

            System.out.println("Data retrieved from Reddit API");

            return fetchedPosts.stream().skip(offset).limit(limit).collect(Collectors.toList());
        });
    }


    private String serializePost(RedditPost post) {
        try {
            return new ObjectMapper().writeValueAsString(post);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize Reddit Post", e);
        }
    }
}
