package org.adi.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.adi.models.RedditPost;
import org.adi.service.MongoService;
import org.adi.service.OpenSearchService;
import org.adi.service.RedditService;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

// endpoint for our application (localhost:8080)
@Path("/reddit")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RedditResource {

    @Inject
    RedditService redditService;

    @Inject
    OpenSearchService openSearchService;

    @Inject
    MongoService mongoService;

    @ConfigProperty(name = "reddit.client-id")
    String clientId;

    @ConfigProperty(name = "reddit.client-secret")
    String clientSecret;

    //  Fetch posts
    //  localhost:8080/user/{username}/posts
    @GET
    @Path("/user/{username}/posts")
    public List<RedditPost> getUserPosts(
            @PathParam("username") String username,
            @QueryParam("limit") @DefaultValue("100") Integer limit,
            @QueryParam("forceFetchFromReddit") @DefaultValue("0") Integer forceFetch   // default value is set if query param is not provided
    ) {
        return redditService.getUserPosts(username, clientId, clientSecret, limit, forceFetch);
    }

    //  Search through indexed posts
    // localhost:8080/search?query="Query entered here"
    @GET
    @Path("/search")
    public List<RedditPost> searchPosts(@QueryParam("query") String query){
        return openSearchService.searchPosts(query);
    }


    // Get top-authors
    //
    @GET
    @Path("/top-authors")
    public List<Document> getTopAuthors(@QueryParam("limit") Integer limit){
        if(limit == null){
            return mongoService.getTopAuthors(10);
        }
        return mongoService.getTopAuthors(limit);
    }
}
