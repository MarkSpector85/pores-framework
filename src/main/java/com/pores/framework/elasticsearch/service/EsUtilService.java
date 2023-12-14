package com.pores.framework.elasticsearch.service;

import com.pores.framework.elasticsearch.dto.SearchCriteria;
import com.pores.framework.elasticsearch.dto.SearchResult;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.util.Map;

/**
 * @author Manas Mohan Swain
 * @version 1.0
 * @since 2023-12-14
 */
public interface EsUtilService {

  RestStatus addDocument(
      String esIndexName,
      String type,
      String id,
      Map<String, Object> document,
      String requiredJsonFilePath);

  RestStatus updateDocument(
      String index,
      String indexType,
      String entityId,
      Map<String, Object> document,
      String requiredJsonFilePath);

  void deleteDocument(String documentId, String esIndexName);

  void deleteDocumentsByCriteria(String esIndexName, SearchSourceBuilder sourceBuilder);

  SearchResult searchDocuments(String esIndexName, SearchCriteria searchCriteria);
}
