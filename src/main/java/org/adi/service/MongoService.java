package org.adi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.adi.config.Constants;
import org.adi.models.RedditPost;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class MongoService {
    @Inject
    MongoClient mongoClient;

    private static final String DATABASE_NAME = "reddit_db";
    private static final String COLLECTION_NAME = "user_posts";

    public MongoService(MongoClient mongoClient){
        this.mongoClient = mongoClient;
    }

    @WithSpan
    public List<RedditPost> getPostsFromDatabase(String username, int limit, int offset){
        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);

        List<RedditPost> posts = new ArrayList<>();
        for(Document doc : collection.find(new Document("author", username))
                .skip(offset)
                .limit(limit)
        ){
            posts.add(mapDocumentToPost(doc));
        }

        return posts;
    }

    @WithSpan
    public Boolean savePostsToDatabase(RedditPost post){
        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);

        // find if the same entry already exists in the database (using URL as it is unique)
        Document existingDocument = collection.find(Filters.eq("url", post.getUrl())).first();

        // add only if the document does not already exist
        if(existingDocument == null){
            System.out.println("Added to Mongo");
            collection.insertOne(mapPostsToDocument(post));
            return false;
        }
        return true;    // true when the document already exists
    }


    // TODO: offset and limit for pagination (to ensure we don't fetch all posts at once in high load situations)
    // teams get knowledge entity

    @WithSpan
    public List<Document> getTopAuthors(int offset, int limit){
        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);

        List<Document> pipeline = new ArrayList<>();

        // group documents by author and count posts by each author
        pipeline.add(new Document("$group", new Document("_id", "$author")
                .append(Constants.TOP_AUTHORS_COLUMN, new Document("$sum", 1))));

        // sort authors by post count in descending order
        pipeline.add(new Document("$sort", new Document(Constants.TOP_AUTHORS_COLUMN, -1)));

        // Skip the specified number of documents for pagination
        pipeline.add(new Document("$skip", offset));

        // limit results to top N authors
        pipeline.add(new Document("$limit", limit));

        // execute the aggregation
        AggregateIterable<Document> results = collection.aggregate(pipeline);

        List<Document> topAuthors = new ArrayList<>();
        for (Document doc : results){
            topAuthors.add(doc);
        }

        return topAuthors;
    }


    private Document mapPostsToDocument(RedditPost post) {
        // we can also use .append() and put it all in 1 line

        Document document = new Document();
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
