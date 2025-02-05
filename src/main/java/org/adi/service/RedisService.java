package org.adi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.adi.models.RedditPost;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class RedisService {

    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;

    @Inject
    public RedisService(ObjectMapper objectMapper) {
        this.jedisPool = new JedisPool("localhost", 6379);
        this.objectMapper = objectMapper;
    }

    public List<RedditPost> getPostsFromCache(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get(key);
            if (json == null) {
                return null;
            }

            return Arrays.asList(objectMapper.readValue(json, RedditPost[].class));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setPostsToCache(String key, List<RedditPost> posts, int ttlSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = objectMapper.writeValueAsString(posts);
            jedis.set(key, json);
            jedis.expire(key, ttlSeconds);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }


    public String buildRedisKey(String username, int limit, int offset) {
        return "posts:" + username + ":limit:" + limit + ":offset:" + offset;
    }
}
