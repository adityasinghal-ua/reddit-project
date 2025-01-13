package org.adi.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Learning: used to ignore properties that are not mapped to the Java object but exist in the Json response
@JsonIgnoreProperties(ignoreUnknown = true)
public class RedditPost {
    private String title;
    private String selftext;
    private String url;
    private String author;
    private String subreddit;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSelftext() {
        return selftext;
    }

    public void setSelftext(String selftext) {
        this.selftext = selftext;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSubreddit() {
        return subreddit;
    }

    public void setSubreddit(String subreddit) {
        this.subreddit = subreddit;
    }

    @Override
    public String toString() {
        return "RedditPost{" +
                "title='" + title + '\'' +
                ", content='" + selftext + '\'' +
                ", author='" + author + '\'' +
                '}';
    }

}
