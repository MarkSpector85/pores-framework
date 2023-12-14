package com.pores.framework.elasticsearch.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.pores.framework.constant.Constants;
import com.pores.framework.elasticsearch.dto.FacetDTO;
import com.pores.framework.elasticsearch.dto.SearchCriteria;
import com.pores.framework.elasticsearch.dto.SearchResult;
import com.pores.framework.elasticsearch.service.EsUtilService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Service implementation for Elasticsearch utility operations. Provides methods for adding,
 * updating, and deleting documents in Elasticsearch, as well as searching for documents based on
 * specified criteria.
 *
 * <p>This service also includes functionality for handling JSON schema validation during document
 * addition and updating. It supports pagination, sorting, and filtering based on provided search
 * criteria, and it can execute bulk deletion of documents based on a search criteria.
 *
 * <p>Uses the Elasticsearch REST High-Level Client for communication with the Elasticsearch
 * cluster.
 *
 * @author Manas Mohan Swain
 * @version 1.0
 * @since 2023-12-14
 */
@Service
@Slf4j
@SuppressWarnings("deprecation")
public class EsUtilServiceImpl implements EsUtilService {

  @Autowired private RestHighLevelClient elasticsearchClient;
  @Autowired private ObjectMapper objectMapper;

  /**
   * Adds a document to the specified Elasticsearch index after validating against a JSON schema.
   * Returns the status of the operation.
   *
   * @param esIndexName The name of the Elasticsearch index.
   * @param type The type of the document.
   * @param id The unique identifier of the document.
   * @param document The document to be added.
   * @param requiredJsonFilePath The path to the required JSON schema file for validation.
   * @return The status of the operation (HTTP status).
   */
  @Override
  public RestStatus addDocument(
      String esIndexName,
      String type,
      String id,
      Map<String, Object> document,
      String requiredJsonFilePath) {
    try {
      JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance();
      InputStream schemaStream = schemaFactory.getClass().getResourceAsStream(requiredJsonFilePath);
      Map<String, Object> map = objectMapper.readValue(schemaStream, new TypeReference<>() {});
      Iterator<Map.Entry<String, Object>> iterator = document.entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<String, Object> entry = iterator.next();
        String key = entry.getKey();
        if (!map.containsKey(key)) {
          iterator.remove();
        }
      }
      IndexRequest indexRequest =
          new IndexRequest(esIndexName, type, id).source(document, XContentType.JSON);
      IndexResponse response = elasticsearchClient.index(indexRequest, RequestOptions.DEFAULT);
      return response.status();
    } catch (Exception e) {
      log.error("Issue while Indexing to es: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Updates a document in the specified Elasticsearch index after validating against a JSON schema.
   * Returns the status of the operation.
   *
   * @param index The name of the Elasticsearch index.
   * @param indexType The type of the document.
   * @param entityId The unique identifier of the document to be updated.
   * @param updatedDocument The updated document.
   * @param requiredJsonFilePath The path to the required JSON schema file for validation.
   * @return The status of the operation (HTTP status).
   */
  @Override
  public RestStatus updateDocument(
      String index,
      String indexType,
      String entityId,
      Map<String, Object> updatedDocument,
      String requiredJsonFilePath) {
    try {
      JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance();
      InputStream schemaStream = schemaFactory.getClass().getResourceAsStream(requiredJsonFilePath);
      Map<String, Object> map = objectMapper.readValue(schemaStream, new TypeReference<>() {});
      Iterator<Map.Entry<String, Object>> iterator = updatedDocument.entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<String, Object> entry = iterator.next();
        String key = entry.getKey();
        if (!map.containsKey(key)) {
          iterator.remove();
        }
      }
      IndexRequest indexRequest =
          new IndexRequest(index)
              .id(entityId)
              .source(updatedDocument)
              .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
      IndexResponse response = elasticsearchClient.index(indexRequest, RequestOptions.DEFAULT);
      return response.status();
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Deletes a document from the specified Elasticsearch index.
   *
   * @param documentId The unique identifier of the document to be deleted.
   * @param esIndexName The name of the Elasticsearch index.
   */
  @Override
  public void deleteDocument(String documentId, String esIndexName) {
    try {
      DeleteRequest request = new DeleteRequest(esIndexName, Constants.INDEX_TYPE, documentId);
      DeleteResponse response = elasticsearchClient.delete(request, RequestOptions.DEFAULT);
      if (response.getResult() == DocWriteResponse.Result.DELETED) {
        log.info("Document deleted successfully from elasticsearch.");
      } else {
        log.error("Document not found or failed to delete from elasticsearch.");
      }
    } catch (Exception e) {
      log.error("Error occurred during deleting document in elasticsearch");
    }
  }

  /**
   * Searches for documents in the specified Elasticsearch index based on the provided search
   * criteria. Returns a paginated result along with facet information.
   *
   * @param esIndexName The name of the Elasticsearch index.
   * @param searchCriteria The search criteria.
   * @return The search result containing paginated data, facets, and total count.
   */
  @Override
  public SearchResult searchDocuments(String esIndexName, SearchCriteria searchCriteria) {
    SearchSourceBuilder searchSourceBuilder = buildSearchSourceBuilder(searchCriteria);
    SearchRequest searchRequest = new SearchRequest(esIndexName);
    searchRequest.source(searchSourceBuilder);
    try {
      if (searchSourceBuilder != null) {
        int pageNumber = searchCriteria.getPageNumber();
        int pageSize = searchCriteria.getPageSize();
        searchSourceBuilder.from(pageNumber);
        if (pageSize != 0) {
          searchSourceBuilder.size(pageSize);
        }
      }
      SearchResponse paginatedSearchResponse =
          elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
      List<Map<String, Object>> paginatedResult = extractPaginatedResult(paginatedSearchResponse);
      Map<String, List<FacetDTO>> fieldAggregations =
          extractFacetData(paginatedSearchResponse, searchCriteria);
      SearchResult searchResult = new SearchResult();
      searchResult.setData(objectMapper.valueToTree(paginatedResult));
      searchResult.setFacets(fieldAggregations);
      searchResult.setTotalCount(paginatedSearchResponse.getHits().getTotalHits().value);
      return searchResult;
    } catch (IOException e) {
      log.error("Error while fetching details from elastic search");
      return null;
    }
  }

  /**
   * Deletes documents from the Elasticsearch index based on the provided search criteria.
   *
   * @param esIndexName The name of the Elasticsearch index.
   * @param sourceBuilder The search criteria for identifying documents to delete.
   */
  @Override
  public void deleteDocumentsByCriteria(String esIndexName, SearchSourceBuilder sourceBuilder) {
    try {
      SearchHits searchHits = executeSearch(esIndexName, sourceBuilder);
      if (searchHits.getTotalHits().value > 0) {
        BulkResponse bulkResponse = deleteMatchingDocuments(esIndexName, searchHits);
        if (!bulkResponse.hasFailures()) {
          log.info("Documents matching the criteria deleted successfully from Elasticsearch.");
        } else {
          log.error("Some documents failed to delete from Elasticsearch.");
        }
      } else {
        log.info("No documents match the criteria.");
      }
    } catch (Exception e) {
      log.error("Error occurred during deleting documents by criteria from Elasticsearch.", e);
    }
  }

  /**
   * Extracts facet data from the given SearchResponse based on the provided search criteria.
   *
   * @param searchResponse The SearchResponse containing aggregation results.
   * @param searchCriteria The search criteria containing facet information.
   * @return A map associating each facet field with a list of FacetDTO objects.
   */
  private Map<String, List<FacetDTO>> extractFacetData(
      SearchResponse searchResponse, SearchCriteria searchCriteria) {
    Map<String, List<FacetDTO>> fieldAggregations = new HashMap<>();
    if (searchCriteria.getFacets() != null) {
      for (String field : searchCriteria.getFacets()) {
        Terms fieldAggregation = searchResponse.getAggregations().get(field + "_agg");
        List<FacetDTO> fieldValueList = new ArrayList<>();
        for (Terms.Bucket bucket : fieldAggregation.getBuckets()) {
          if (!bucket.getKeyAsString().isEmpty()) {
            FacetDTO facetDTO = new FacetDTO(bucket.getKeyAsString(), bucket.getDocCount());
            fieldValueList.add(facetDTO);
          }
        }
        fieldAggregations.put(field, fieldValueList);
      }
    }
    return fieldAggregations;
  }

  /**
   * Extracts paginated search results from the given SearchResponse.
   *
   * @param paginatedSearchResponse The SearchResponse containing paginated search results.
   * @return A list of maps representing the extracted paginated results.
   */
  private List<Map<String, Object>> extractPaginatedResult(SearchResponse paginatedSearchResponse) {
    SearchHit[] hits = paginatedSearchResponse.getHits().getHits();
    List<Map<String, Object>> paginatedResult = new ArrayList<>();
    for (SearchHit hit : hits) {
      paginatedResult.add(hit.getSourceAsMap());
    }
    return paginatedResult;
  }

  /**
   * Builds the search query based on the provided search criteria. Returns a SearchSourceBuilder
   * for executing the Elasticsearch search request.
   *
   * @param searchCriteria The search criteria.
   * @return The constructed SearchSourceBuilder.
   */
  private SearchSourceBuilder buildSearchSourceBuilder(SearchCriteria searchCriteria) {
    log.info("Building search query");
    if (searchCriteria == null || searchCriteria.toString().isEmpty()) {
      log.error("Search criteria body is missing");
      return null;
    }
    BoolQueryBuilder boolQueryBuilder = buildFilterQuery(searchCriteria.getFilterCriteriaMap());
    addQueryStringToFilter(searchCriteria.getSearchString(), boolQueryBuilder);
    buildQueryForRange(boolQueryBuilder, searchCriteria);
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    searchSourceBuilder.query(boolQueryBuilder);
    addSortToSearchSourceBuilder(searchCriteria, searchSourceBuilder);
    addRequestedFieldsToSearchSourceBuilder(searchCriteria, searchSourceBuilder);
    addFacetsToSearchSourceBuilder(searchCriteria.getFacets(), searchSourceBuilder);
    return searchSourceBuilder;
  }

  /**
   * Constructs a range query for Elasticsearch based on the range criteria in the search criteria.
   *
   * @param boolQueryBuilder The BoolQueryBuilder to which the range query is added.
   * @param searchCriteria The search criteria containing range criteria.
   */
  private void buildQueryForRange(
      BoolQueryBuilder boolQueryBuilder, SearchCriteria searchCriteria) {
    try {
      if (searchCriteria != null && searchCriteria.getRangeCriteriaList() != null) {
        for (SearchCriteria.RangeCriterion rangeCriterion : searchCriteria.getRangeCriteriaList()) {
          String field = rangeCriterion.getField();
          SearchCriteria.Range range = rangeCriterion.getRange();

          if (range != null && range.getMin() != null && range.getMax() != null) {
            BoolQueryBuilder rangeQuery = QueryBuilders.boolQuery();
            rangeQuery.should(
                QueryBuilders.rangeQuery(field).from(range.getMin()).to(range.getMax()));
            boolQueryBuilder.must(rangeQuery);
          }
        }
      }
    } catch (Exception e) {
      throw new UnknownFormatConversionException(
          "Unsupported range format. Please provide a valid format.");
    }
  }

  /**
   * Builds a filter query based on the filter criteria map.
   *
   * @param filterCriteriaMap The map containing filter criteria.
   * @return The constructed BoolQueryBuilder representing the filter query.
   */
  private BoolQueryBuilder buildFilterQuery(Map<String, Object> filterCriteriaMap) {
    BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
    if (filterCriteriaMap != null) {
      filterCriteriaMap.forEach(
          (field, value) -> {
            if (value instanceof Boolean) {
              boolQueryBuilder.must(QueryBuilders.termQuery(field, value));
            } else if (value instanceof ArrayList) {
              boolQueryBuilder.must(
                  QueryBuilders.termsQuery(
                      field + Constants.KEYWORD, ((ArrayList<?>) value).toArray()));
            } else if (value instanceof String) {
              boolQueryBuilder.must(QueryBuilders.termsQuery(field + Constants.KEYWORD, value));
            }
          });
    }
    return boolQueryBuilder;
  }

  /**
   * Adds requested fields to the SearchSourceBuilder based on the provided search criteria.
   *
   * @param searchCriteria The search criteria containing requested fields.
   * @param searchSourceBuilder The SearchSourceBuilder to which requested fields are added.
   */
  private void addSortToSearchSourceBuilder(
      SearchCriteria searchCriteria, SearchSourceBuilder searchSourceBuilder) {
    if (isNotBlank(searchCriteria.getOrderBy()) && isNotBlank(searchCriteria.getOrderDirection())) {
      SortOrder sortOrder =
          Constants.ASC.equals(searchCriteria.getOrderDirection()) ? SortOrder.ASC : SortOrder.DESC;
      searchSourceBuilder.sort(
          SortBuilders.fieldSort(searchCriteria.getOrderBy() + Constants.KEYWORD).order(sortOrder));
    }
  }

  /**
   * Adds requested fields to the SearchSourceBuilder based on the provided search criteria. If no
   * specific fields are requested, all fields will be included in the response.
   *
   * @param searchCriteria The search criteria containing requested fields.
   * @param searchSourceBuilder The SearchSourceBuilder to which requested fields are added.
   */
  private void addRequestedFieldsToSearchSourceBuilder(
      SearchCriteria searchCriteria, SearchSourceBuilder searchSourceBuilder) {
    if (searchCriteria.getRequestedFields() == null) {
      // Get all fields in response
      searchSourceBuilder.fetchSource(null);
    } else {
      if (searchCriteria.getRequestedFields().isEmpty()) {
        log.error("Please specify at least one field to include in the results.");
      }
      searchSourceBuilder.fetchSource(
          searchCriteria.getRequestedFields().toArray(new String[0]), null);
    }
  }

  /**
   * Adds a wildcard query to the provided BoolQueryBuilder based on the search string.
   *
   * @param searchString The search string.
   * @param boolQueryBuilder The BoolQueryBuilder to which the wildcard query is added.
   */
  private void addQueryStringToFilter(String searchString, BoolQueryBuilder boolQueryBuilder) {
    if (isNotBlank(searchString)) {
      boolQueryBuilder.must(
          QueryBuilders.boolQuery()
              .should(new WildcardQueryBuilder("searchTags.keyword", "*" + searchString + "*")));
    }
  }

  /**
   * Adds facet aggregations to the SearchSourceBuilder based on the provided facet list.
   *
   * @param facets The list of facets.
   * @param searchSourceBuilder The SearchSourceBuilder to which facet aggregations are added.
   */
  private void addFacetsToSearchSourceBuilder(
      List<String> facets, SearchSourceBuilder searchSourceBuilder) {
    if (facets != null) {
      for (String field : facets) {
        searchSourceBuilder.aggregation(
            AggregationBuilders.terms(field + "_agg").field(field + ".keyword").size(250));
      }
    }
  }

  /**
   * Checks if a string is not blank (null or empty).
   *
   * @param value The string to check.
   * @return True if the string is not blank, false otherwise.
   */
  private boolean isNotBlank(String value) {
    return value != null && !value.trim().isEmpty();
  }

  /**
   * Executes a search request in Elasticsearch and returns the search hits.
   *
   * @param esIndexName The name of the Elasticsearch index.
   * @param sourceBuilder The SearchSourceBuilder representing the search criteria.
   * @return The SearchHits resulting from the search request.
   * @throws IOException If an I/O error occurs during the search request.
   */
  private SearchHits executeSearch(String esIndexName, SearchSourceBuilder sourceBuilder)
      throws IOException {
    SearchRequest searchRequest = new SearchRequest(esIndexName);
    searchRequest.source(sourceBuilder);
    SearchResponse searchResponse =
        elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
    return searchResponse.getHits();
  }

  /**
   * Deletes documents from Elasticsearch based on the provided SearchHits.
   *
   * @param esIndexName The name of the Elasticsearch index.
   * @param searchHits The SearchHits representing documents to be deleted.
   * @return The BulkResponse containing the result of the deletion operation.
   * @throws IOException If an I/O error occurs during the deletion operation.
   */
  private BulkResponse deleteMatchingDocuments(String esIndexName, SearchHits searchHits)
      throws IOException {
    BulkRequest bulkRequest = new BulkRequest();
    searchHits.forEach(
        hit -> bulkRequest.add(new DeleteRequest(esIndexName, Constants.INDEX_TYPE, hit.getId())));
    return elasticsearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
  }
}
