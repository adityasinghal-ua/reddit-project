package org.adi.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.adi.models.RedditPost;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.transport.rest_client.RestClientTransport;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class OpenSearchService {
    private final OpenSearchClient openSearchClient;

    public OpenSearchService(){
        RestClient restClient = RestClient.builder(new org.apache.http.HttpHost("localhost", 9200)).build();
        this.openSearchClient = new OpenSearchClient(new RestClientTransport(restClient, new JacksonJsonpMapper()));
    }

    public void indexPost(RedditPost post){
        try{
            IndexRequest<RedditPost> indexRequest = new IndexRequest.Builder<RedditPost>()
                    .index("reddit_posts")    // specifies the index/collection name
                    .id(post.getUrl())              // uses the post URL as unique identifier
                    .document(post)                 // the entire post object is to indexed
                    .build();                       // constructs the index request
            openSearchClient.index(indexRequest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to index post", e);
        }
    }

    public List<RedditPost> searchPosts(String query){
        try{
            // build the search request that is to be passed to the openSearchClient to search
            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index("reddit_posts")      // target index
                    .query(q -> q.multiMatch(m -> m
                            .fields("title", "selftext", "url", "author", "subreddit")  // multiple search fields
                            .query(query)))             // search query
                    .build();
            SearchResponse<RedditPost> response = openSearchClient.search(searchRequest, RedditPost.class);

            return response.hits().hits().stream()
                    .map(hit -> hit.source())   // extracts source objects from search hits
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to search posts", e);
        }
    }

}
