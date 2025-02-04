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

    public CompletableFuture<List<RedditPost>> getUserPosts(
            String username,
            String clientId,
            String clientSecret,
            Integer limit,
            Integer offset
    ) {
        // made placeholders because you cannot reassign the value of a variable in a lambda
        final int[] limitHolder = { limit };
        final int[] offsetHolder = { offset };

        return getCachedPosts(username, limitHolder[0], offsetHolder[0])
                .thenCompose(cachedPosts -> {
                    // If enough posts in cache, return them
                    if (cachedPosts != null && !cachedPosts.isEmpty() && cachedPosts.size() == limitHolder[0]) {
                        System.out.println("Data retrieved from MongoDB");
                        return CompletableFuture.completedFuture(cachedPosts);
                    }

                    List<RedditPost> resultPosts = new ArrayList<>();
                    if (cachedPosts != null) {
                        resultPosts.addAll(cachedPosts);
                        // Modify the array contents
                        limitHolder[0] -= cachedPosts.size();
                        offsetHolder[0] += cachedPosts.size();
                    }
                    // Now fetch from Reddit
                    return getAccessToken(clientId, clientSecret).thenCompose(accessToken ->
                            fetchPostsFromReddit(username, accessToken, limitHolder[0], offsetHolder[0])
                    ).thenApply(fetchedPosts -> {
                        // Publish new posts to Kafka
                        fetchedPosts.forEach(post ->
                                kafkaProducer.sendMessage(Constants.REDDIT_TOPIC, username, serializePost(post))
                        );
                        resultPosts.addAll(fetchedPosts);
                        return resultPosts;
                    });
                });
    }


    private CompletableFuture<List<RedditPost>> getCachedPosts(String username, Integer limit, Integer offset) {
        return CompletableFuture.supplyAsync(() -> {
            List<RedditPost> posts = mongoService.getPostsFromDatabase(username, limit, offset);
            if (posts != null && !posts.isEmpty()) {
                return posts;
            } else {
                System.out.println("Data not in Mongo");
                return null;
            }
        });
    }


    private CompletableFuture<String> getAccessToken(String clientId, String clientSecret) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String basicAuth = "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
                String tokenResponse = authClient.getAccessToken(basicAuth, "client_credentials");
                // parse the JSON to extract "access_token"
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(tokenResponse);
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


    private String serializePost(RedditPost post){
        try {
            return new ObjectMapper().writeValueAsString(post);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize Reddit Post", e);
        }
    }
}
