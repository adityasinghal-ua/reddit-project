package org.adi.service;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.adi.config.Constants;
import org.adi.models.RedditPost;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.transport.rest_client.RestClientTransport;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class OpenSearchService {
    private OpenSearchClient openSearchClient;

    @ConfigProperty(name = "opensearch.host")
    String openSearchHost;

    @ConfigProperty(name = "opensearch.port")
    Integer openSearchPort;

    @PostConstruct
    public void init() {
        RestClient restClient = RestClient.builder(new org.apache.http.HttpHost(openSearchHost, openSearchPort)).build();
        this.openSearchClient = new OpenSearchClient(new RestClientTransport(restClient, new JacksonJsonpMapper()));
    }

    @WithSpan
    public void indexPost(RedditPost post){
        try{
            IndexRequest<RedditPost> indexRequest = new IndexRequest.Builder<RedditPost>()
                    .index(Constants.OPENSEARCH_INDEX_NAME)    // specifies the index/collection name
                    .id(post.getUrl())              // uses the post URL as unique identifier
                    .document(post)                 // the entire post object is to indexed
                    .build();                       // constructs the index request
            System.out.println("Added to OpenSearch");
            openSearchClient.index(indexRequest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to index post", e);
        }
    }

    @WithSpan
    public List<RedditPost> searchPosts(String query, Integer limit, Integer offset){
        try{
            // build the search request that is to be passed to the openSearchClient to search
            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index(Constants.OPENSEARCH_INDEX_NAME)      // target index
                    .query(q -> q.multiMatch(m -> m
                            .fields("title", "selftext", "url", "author", "subreddit")  // multiple search fields
                            .query(query)))             // search query
                    .from(offset)
                    .size(limit)
                    .build();
            SearchResponse<RedditPost> response = openSearchClient.search(searchRequest, RedditPost.class);

            return response.hits().hits().stream()      //.hits() gives HitsMetadata, .hits().hits() gives the actual list of Hit objects
                    .map(hit -> hit.source())   // extracts source objects from search hits
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to search posts", e);
        }
    }

    public List<RedditPost> fuzzySearchPosts(String query, Integer limit, Integer offset){
        try{
            // build the search request that is to be passed to the openSearchClient to search
            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index(Constants.OPENSEARCH_INDEX_NAME)      // target index
                    .query(q -> q.multiMatch(m -> m
                            .fields("title", "selftext", "url", "author", "subreddit")  // multiple search fields
                            .query(query)
                            .fuzziness("AUTO")
                            ))
                    .from(offset)
                    .size(limit)
                    .build();
            SearchResponse<RedditPost> response = openSearchClient.search(searchRequest, RedditPost.class);

            return response.hits().hits().stream()      //.hits() gives HitsMetadata, .hits().hits() gives the actual list of Hit objects
                    .map(hit -> hit.source())   // extracts source objects from search hits
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to search posts", e);
        }
    }

}
