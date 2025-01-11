package org.adi.controller.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

// learning
@RegisterRestClient(configKey = "reddit-auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_FORM_URLENCODED) // reddit api expects data in this format for access token
public interface RedditAuthClient {
    @POST
    @Path("/api/v1/access_token")
    String getAccessToken(
            // learning
            @HeaderParam("Authorization") String basicAuth,
//            actual header entry from Postman
//            Authorization: Basic ZFhVRGRhVjJaYW1NbmJXV2F5UUs0UTpPQ1ZrZjUyeW1lTW9BaUFiZWxJc2JVRVVsaUFBRVE=
            @FormParam("grant_type") String grantType
    );
}
