package org.adi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.adi.models.RedditPost;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bson.Document;
import org.eclipse.microprofile.reactive.messaging.Incoming;

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

    public List<RedditPost> getPostsFromDatabase(String username){
        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);

        List<RedditPost> posts = new ArrayList<>();
        for(Document doc : collection.find(new Document("author", username))){
            posts.add(mapDocumentToPost(doc));
        }

        return posts;
    }

    public Boolean savePostsToDatabase(RedditPost post){
        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);

        // find if the same entry already exists
        Document existingDocument = collection.find(Filters.eq("url", post.getUrl())).first();

        // add only if the document does not already exist
        if(existingDocument == null){
            System.out.println("Added to Mongo");
            collection.insertOne(mapPostsToDocument(post));
            return false;
        }
        return true;    // true when the document already exists
    }


    public List<Document> getTopAuthors(int limit){
        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);

        List<Document> pipeline = new ArrayList<>();

        // group documents by author and count posts by each author
        pipeline.add(new Document("$group", new Document("_id", "$author")
                .append("postCount", new Document("$sum", 1))));

        // sort authors by post count in descending order
        pipeline.add(new Document("$sort", new Document("postCount", -1)));

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
