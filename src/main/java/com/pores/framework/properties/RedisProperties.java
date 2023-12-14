package com.pores.framework.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for connecting to a Redis server.
 *
 * <p>This class represents the configuration properties needed to establish a connection to a Redis
 * server. It uses the `@ConfigurationProperties` annotation to bind properties prefixed with
 * `pores.framework.redis` from the application configuration files.
 *
 * <p>The properties include the Redis host, port, and optional password for authentication. It also
 * provides additional pool configuration properties for fine-tuning the connection pool.
 *
 * @author Manas Mohan Swain
 * @version 1.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "pores.framework.redis")
public class RedisProperties {
  private String host = "localhost";
  private int port = 6379;
  private boolean passwordRequired = false;
  private String password;

  // Pool configuration properties
  private int maxIdle = 128;
  private int maxTotal = 3000;
  private int minIdle = 100;
  private boolean testOnBorrow = true;
  private boolean testOnReturn = true;
  private boolean testWhileIdle = true;
  private long minEvictableIdleTime = 120000;
  private long timeBetweenEvictionRuns = 30000;
  private int numTestsPerEvictionRun = 3;
  private boolean blockWhenExhausted = true;
}
