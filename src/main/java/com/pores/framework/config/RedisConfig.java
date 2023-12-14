package com.pores.framework.config;

import com.pores.framework.properties.RedisProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Configuration class for setting up Redis connection pool and related properties.
 *
 * <p>Configures a JedisPool to manage connections to the Redis server based on the provided
 * properties.
 *
 * <p>This class is responsible for creating and configuring the JedisPool used for interacting with
 * the Redis server. It reads properties from the RedisProperties class to customize the behavior of
 * the connection pool.
 *
 * @author Manas Mohan Swain
 * @version 1.0
 */
@Configuration
@EnableCaching
public class RedisConfig {

  private final RedisProperties redisProperties;

  /**
   * Constructor for RedisConfig class.
   *
   * @param redisProperties The properties related to Redis configuration.
   */
  @Autowired
  public RedisConfig(RedisProperties redisProperties) {
    this.redisProperties = redisProperties;
  }

  /**
   * Creates and configures a JedisPool based on the provided properties.
   *
   * @return JedisPool instance configured for Redis connection.
   * @throws IllegalStateException If Redis password is required but not provided.
   */
  @Bean
  public JedisPool jedisPool() {
    JedisPoolConfig poolConfig = getJedisPoolConfig();
    JedisPool jedisPool;
    if (redisProperties.isPasswordRequired()) {
      String password = redisProperties.getPassword();
      if (!StringUtils.hasText(password)) {
        throw new IllegalStateException("Redis password is required but not provided.");
      }
      jedisPool =
          new JedisPool(
              poolConfig, redisProperties.getHost(), redisProperties.getPort(), 0, password);
    } else {
      jedisPool = new JedisPool(poolConfig, redisProperties.getHost(), redisProperties.getPort());
    }

    return jedisPool;
  }

  /**
   * Creates and configures a JedisPoolConfig based on the provided properties.
   *
   * @return JedisPoolConfig instance with configured properties.
   */
  private JedisPoolConfig getJedisPoolConfig() {
    JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxIdle(redisProperties.getMaxIdle());
    poolConfig.setMaxTotal(redisProperties.getMaxTotal());
    poolConfig.setMinIdle(redisProperties.getMinIdle());
    poolConfig.setTestOnBorrow(redisProperties.isTestOnBorrow());
    poolConfig.setTestOnReturn(redisProperties.isTestOnReturn());
    poolConfig.setTestWhileIdle(redisProperties.isTestWhileIdle());
    poolConfig.setMinEvictableIdleTime(
        Duration.ofMillis(redisProperties.getMinEvictableIdleTime()));
    poolConfig.setTimeBetweenEvictionRuns(
        Duration.ofMillis(redisProperties.getTimeBetweenEvictionRuns()));
    poolConfig.setNumTestsPerEvictionRun(redisProperties.getNumTestsPerEvictionRun());
    poolConfig.setBlockWhenExhausted(redisProperties.isBlockWhenExhausted());
    return poolConfig;
  }
}
