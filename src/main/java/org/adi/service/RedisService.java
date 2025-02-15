package org.adi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.adi.models.RedditPost;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class RedisService {

    private JedisPool jedisPool;
    private ObjectMapper objectMapper;

    @Inject
    public RedisService(ObjectMapper objectMapper,
                        @ConfigProperty(name = "redis.host", defaultValue = "localhost") String redisHost,
                        @ConfigProperty(name = "redis.port", defaultValue = "6379") int redisPort) {
        this.jedisPool = new JedisPool(redisHost, redisPort);
        this.objectMapper = objectMapper;
    }

    @WithSpan
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

    @WithSpan
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
