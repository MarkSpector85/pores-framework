package com.pores.framework.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pores.framework.constant.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Service class for interacting with Redis cache. This class provides methods to perform operations
 * such as putting, getting, and deleting data from the Redis cache. Uses a JedisPool for managing
 * Jedis connections to the Redis server.
 *
 * @author Manas Mohan Swain
 * @version 1.0
 */
@Service
@Slf4j
public class CacheService {

  @Autowired private JedisPool jedisPool;
  @Autowired private ObjectMapper objectMapper;

  /**
   * Retrieves a Jedis instance from the JedisPool.
   *
   * @return Jedis instance.
   */
  public Jedis getJedis() {
    try (Jedis jedis = jedisPool.getResource()) {
      return jedis;
    }
  }

  /**
   * Puts data into the Redis cache with a specified key.
   *
   * @param key The key for the cache entry.
   * @param object The object to be stored in the cache.
   */
  public void putCache(String key, Object object) {
    try {
      String data = objectMapper.writeValueAsString(object);
      try (Jedis jedis = jedisPool.getResource()) {
        jedis.set(Constants.REDIS_KEY_PREFIX + key, data);
        long cacheTtl = 60;
        jedis.expire(Constants.REDIS_KEY_PREFIX + key, cacheTtl);
      }
    } catch (Exception e) {
      log.error("Error while putting data in Redis cache: {} ", e.getMessage());
    }
  }

  /**
   * Gets data from the Redis cache using a specified key.
   *
   * @param key The key for the cache entry.
   * @return The cached data as a String.
   */
  public String getCache(String key) {
    try {
      return getJedis().get(Constants.REDIS_KEY_PREFIX + key);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Deletes data from the Redis cache using a specified key.
   *
   * @param key The key for the cache entry to be deleted.
   */
  public void deleteCache(String key) {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.del(Constants.REDIS_KEY_PREFIX + key);
    } catch (Exception e) {
      log.error("Error while deleting data from Redis cache: {} ", e.getMessage());
    }
  }
}
