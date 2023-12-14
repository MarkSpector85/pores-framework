package com.pores.framework.elasticsearch.esconfig;

import com.pores.framework.properties.ElasticSearchProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Configuration class for establishing a connection to Elasticsearch using the REST client.
 *
 * <p>This class provides a configuration bean for creating a RestHighLevelClient that connects to
 * an Elasticsearch cluster. It uses properties from the ElasticSearchProperties class to determine
 * the connection details such as host, port, and authentication credentials.
 *
 * @author Manas Mohan Swain
 * @version 1.0
 */
@Configuration
@Slf4j
@SuppressWarnings("deprecation")
public class EsConnection {

  private final ElasticSearchProperties elasticsearchProperties;

  /**
   * Constructor for EsConnection class.
   *
   * @param elasticsearchProperties The properties related to Elasticsearch configuration.
   */
  @Autowired
  public EsConnection(ElasticSearchProperties elasticsearchProperties) {
    this.elasticsearchProperties = elasticsearchProperties;
  }

  /**
   * Creates and configures a RestHighLevelClient for Elasticsearch connection.
   *
   * @return RestHighLevelClient instance configured for Elasticsearch connection.
   */
  @Bean
  public RestHighLevelClient elasticsearchClient() {
    RestClientBuilder builder =
        RestClient.builder(
            new HttpHost(
                elasticsearchProperties.getHost(), elasticsearchProperties.getPort(), "http"));
    if (StringUtils.hasText(elasticsearchProperties.getUsername())
        && StringUtils.hasText(elasticsearchProperties.getPassword())) {
      final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(
          AuthScope.ANY,
          new UsernamePasswordCredentials(
              elasticsearchProperties.getUsername(), elasticsearchProperties.getPassword()));

      builder.setHttpClientConfigCallback(
          httpClientBuilder ->
              httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
    }
    return new RestHighLevelClient(builder);
  }
}
