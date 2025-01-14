package org.adi.controller.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.adi.models.RedditResponseWrapper;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;


// https://oauth.reddit.com/user/{username}/submitted?limit={limit}&after={after}
@RegisterRestClient(configKey = "reddit-user")
public interface RedditUserClient {
    @GET
    @Path("/user/{username}/submitted")
    @Produces(MediaType.APPLICATION_JSON)
    RedditResponseWrapper getUserPosts(
            @HeaderParam("Authorization") String bearerToken,
            @PathParam("username") String username,
            @QueryParam("limit") Integer limit,
            @QueryParam("after") String after
    );
}