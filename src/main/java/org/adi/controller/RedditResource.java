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

import static java.lang.Math.max;
import static java.lang.Math.min;

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
    //  localhost:8089/user/{username}/posts
    @GET
    @Path("/user/{username}/posts")
    public List<RedditPost> getUserPosts(
            @PathParam("username") String username,
            @QueryParam("limit") @DefaultValue("100") Integer limit,
            @QueryParam("offset") @DefaultValue("0") Integer offset
    ) {
        limit = max(min(limit, 100), 0);
        try {
            return redditService.getUserPosts(username, clientId, clientSecret, limit, offset)
                    .get();  // get() blocks until complete, makes async call sync
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //  Search through indexed posts
    // localhost:8089/search?query="Query entered here"
    @GET
    @Path("/search")
    public List<RedditPost> searchPosts(
            @QueryParam("query") String query,
            @QueryParam("offset") @DefaultValue("0") Integer offset,
            @QueryParam("limit") @DefaultValue("100") Integer limit

    ){
        limit = max(min(limit, 100), 0);
        offset= max(0, offset);
        return openSearchService.searchPosts(query, limit, offset);
    }

    @GET
    @Path("/fuzzySearch")
    public List<RedditPost> fuzzySearchPosts(
            @QueryParam("query") String query,
            @QueryParam("offset") @DefaultValue("0") Integer offset,
            @QueryParam("limit") @DefaultValue("100") Integer limit
    ){
        return openSearchService.fuzzySearchPosts(query, limit, offset);
    }


    // Get top-authors
    //
    @GET
    @Path("/top-authors")
    public List<Document> getTopAuthors(
            @QueryParam("offset") @DefaultValue("0") Integer offset,
            @QueryParam("limit") @DefaultValue("10") Integer limit
    ){
        offset = max(offset, 0);
        limit = max(0, limit);
        limit = min(limit, 100);
        return mongoService.getTopAuthors(offset, limit);
    }
}
