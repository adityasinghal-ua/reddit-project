package org.adi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.adi.controller.client.RedditAuthClient;
import org.adi.controller.client.RedditUserClient;
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
    MongoClient mongoClient;

    private static final String DATABASE_NAME = "reddit_db";
    private static final String COLLECTION_NAME = "user_posts";


    public List<RedditPost> getUserPosts(String username, String clientId, String clientSecret){

        // Connect to Mongo and check for already existing posts
        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);

        List<RedditPost> cachedPosts = new ArrayList<>();
        for (Document doc : collection.find(new Document("username", username))){
            cachedPosts.add(mapDocumentToPost(doc));
        }

        if(!cachedPosts.isEmpty()) return cachedPosts;


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

        RedditResponseWrapper responseWrapper = userClient.getUserPosts("Bearer " + accessToken, username);
        List<RedditPost> fetchedPosts = responseWrapper.getData()
                .getChildren().
                stream()
                .map(child -> child.getData())
                .collect(Collectors.toList());

        for (RedditPost post : fetchedPosts){
            collection.insertOne(mapPostsToDocument(post, username));
        }
        return fetchedPosts;
    }

    private Document mapPostsToDocument(RedditPost post, String username) {
        Document document = new Document();
        document.put("username", username);
        document.put("title", post.getTitle());
        document.put("selftext", post.getSelftext());
        document.put("url", post.getUrl());
        document.put("author", post.getAuthor());
        document.put("subreddit", post.getSubreddit());
        return document;
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

}
