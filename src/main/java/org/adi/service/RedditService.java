package org.adi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
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

        if(forceFetch == 0){
            // if posts are present in MongoDB
            List<RedditPost> cachedPosts = mongoService.getPostsFromDatabase(username);

            if(!cachedPosts.isEmpty()){
                cachedPosts = cachedPosts.stream().limit(limit).collect(Collectors.toList());
                System.out.println("Data retrieved from MongoDB");  // logged in terminal
                return cachedPosts;
            }
        }

        System.out.println("Data not in Mongo");

        // if posts are not found in database
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes());

        String tokenResponse = authClient.getAccessToken(basicAuth, "client_credentials");

        String accessToken;
        try{
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(tokenResponse);
            accessToken = jsonNode.get("access_token").asText();

        }catch (Exception e){
            throw new RuntimeException("Failed to get access token", e);
        }

        RedditResponseWrapper responseWrapper;

        try {
            responseWrapper = userClient.getUserPosts("Bearer " + accessToken, username, limit);
            if (responseWrapper == null || responseWrapper.getData() == null || responseWrapper.getData().getChildren().isEmpty()) {
                throw new NotFoundException("No posts found for user: " + username);
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch posts for username " + username + ": " + e.getMessage());
            throw new RuntimeException("Unable to fetch user posts", e);
        }

        List<RedditPost> fetchedPosts = responseWrapper.getData()
                .getChildren().
                stream()
                .map(child -> child.getData())
                .collect(Collectors.toList());

        System.out.println("Data retrieved from Reddit API");  // logged in terminal

//        fetchedPosts.forEach(post -> System.out.println("Title: " + post.getTitle() + ", content: " + post.getSelftext()));

        fetchedPosts.forEach(post -> kafkaProducer.sendMessage("reddit-posts", username, serializePost(post)));
        return fetchedPosts;

    }

    private RedditPost mapDocumentToPost(Document doc) {
        RedditPost post = new RedditPost();
        post.setTitle(doc.getString("title"));
        post.setSelftext(doc.getString("selftext"));
        post.setUrl(doc.getString("url"));
        post.setAuthor(doc.getString("author"));
        post.setSubreddit(doc.getString("subreddit"));
        return post;
    }

    private String serializePost(RedditPost post){
        try{
            return new ObjectMapper().writeValueAsString(post);
        }catch (Exception e){
            throw new RuntimeException("Failed to serialize Reddit Post", e);
        }
    }


}
