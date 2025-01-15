package org.adi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
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
import java.util.stream.Collectors;

@ApplicationScoped
public class RedditService {
    @Inject
    @RestClient
    RedditAuthClient authClient;

    @Inject
    @RestClient
    RedditUserClient userClient;

    @Inject
    MongoService mongoService;

    @Inject
    KafkaProducerService kafkaProducer;

    public List<RedditPost> getUserPosts(String username, String clientId, String clientSecret, Integer limit, Integer forceFetch){
        List<RedditPost> cachedPosts = getCachedPosts(username, limit, forceFetch);
        if(cachedPosts != null){
            return cachedPosts;
        }

        String accessToken = getAccessToken(clientId, clientSecret);
        List<RedditPost> fetchedPosts = fetchPostsFromReddit(username, accessToken, limit);

        fetchedPosts.forEach(post -> kafkaProducer.sendMessage("reddit-posts", username, serializePost(post)));
        return fetchedPosts;
    }

    private List<RedditPost> getCachedPosts(String username, Integer limit, Integer forceFetch) {
        if (forceFetch == 0) {
            List<RedditPost> cachedPosts = mongoService.getPostsFromDatabase(username);
            if (!cachedPosts.isEmpty() && limit != null && cachedPosts.size() >= limit) {
                System.out.println("Data retrieved from MongoDB");
                return cachedPosts.stream().limit(limit).collect(Collectors.toList());
            }
        }
        System.out.println("Data not in Mongo");
        return null;
    }

    private String getAccessToken(String clientId, String clientSecret) {
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
        String tokenResponse = authClient.getAccessToken(basicAuth, "client_credentials");

        try{
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(tokenResponse);
            return jsonNode.get("access_token").asText();
        }catch (Exception e){
            throw new RuntimeException("Failed to get access token", e);
        }
    }

    private List<RedditPost> fetchPostsFromReddit(String username, String accessToken, Integer limit) {
        List<RedditPost> fetchedPosts = new ArrayList<>();
        String after = null;

        try {
            while((after != null || fetchedPosts.isEmpty()) && fetchedPosts.size() < limit) {

                // hard coding limit = 100, as allowed values are [1,100]
                RedditResponseWrapper responseWrapper = userClient.getUserPosts("Bearer " + accessToken, username, 100, after);

                if (responseWrapper == null || responseWrapper.getData() == null || responseWrapper.getData().getChildren().isEmpty()) {
                    throw new NotFoundException("No posts found for user: " + username);
                }

                fetchedPosts.addAll(responseWrapper.getData().getChildren().stream()
                        .map(child -> child.getData())
                        .collect(Collectors.toList()));

                after = responseWrapper.getData().getAfter();
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch posts for username " + username + ": " + e.getMessage());
            throw new RuntimeException("Unable to fetch user posts", e);
        }

        System.out.println("Data retrieved from Reddit API");
        return fetchedPosts.stream().limit(limit).collect(Collectors.toList());
    }


    // returns a JSON string of the RedditPost object
    private String serializePost(RedditPost post){
        try{
            System.out.println(new ObjectMapper().writeValueAsString(post));
            return new ObjectMapper().writeValueAsString(post);
        }catch (Exception e){
            throw new RuntimeException("Failed to serialize Reddit Post", e);
        }
    }


}
