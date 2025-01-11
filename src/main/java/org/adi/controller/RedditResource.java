package org.adi.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.adi.models.RedditPost;
import org.adi.service.RedditService;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

// endpoint for our application (localhost:8080)
@Path("/reddit")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RedditResource {

    @Inject
    RedditService redditService;

    @ConfigProperty(name = "reddit.client-id")
    String clientId;

    @ConfigProperty(name = "reddit.client-secret")
    String clientSecret;

    @GET
    @Path("/user/{username}/posts")
    public List<RedditPost> getUserPosts(@PathParam("username") String username) {
        return redditService.getUserPosts(username, clientId, clientSecret);
    }
}
