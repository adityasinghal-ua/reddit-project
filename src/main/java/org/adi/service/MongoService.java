package org.adi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
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

    @Incoming("reddit-posts")
    public void consumePost(ConsumerRecord<String , String> record){
        try{
            String username = record.key();
            RedditPost post = new ObjectMapper().readValue(record.value(), RedditPost.class);
            savePostsToDatabase(post);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public void savePostsToDatabase(RedditPost post){
        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);

        collection.insertOne(mapPostsToDocument(post));

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
