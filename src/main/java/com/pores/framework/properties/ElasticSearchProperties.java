package com.pores.framework.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for connecting to an Elasticsearch cluster.
 *
 * <p>This class represents the configuration properties needed to establish a connection to an
 * Elasticsearch cluster. It uses the `@ConfigurationProperties` annotation to bind properties
 * prefixed with `pores.framework.elasticsearch` from the application configuration files.
 *
 * <p>The properties include the Elasticsearch host, port, and optional username/password for
 * authentication.
 *
 * @author Manas Mohan Swain
 * @version 1.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "pores.framework.elasticsearch")
public class ElasticSearchProperties {
  private String host = "localhost";
  private int port = 9200;
  private String username;
  private String password;
}
