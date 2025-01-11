package org.adi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.adi.controller.client.RedditAuthClient;
import org.adi.controller.client.RedditUserClient;
import org.adi.models.RedditPost;
import org.adi.models.RedditResponseWrapper;
import org.eclipse.microprofile.rest.client.inject.RestClient;

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

    public List<RedditPost> getUserPosts(String username, String clientId, String clientSecret){
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
        return responseWrapper.getData()
                .getChildren().
                stream()
                .map(child -> child.getData())
                .collect(Collectors.toList());
    }

}
