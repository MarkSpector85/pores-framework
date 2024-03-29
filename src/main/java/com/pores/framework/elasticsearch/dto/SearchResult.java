package com.pores.framework.elasticsearch.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author Manas Mohan Swain
 * @version 1.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SearchResult implements Serializable {

  private JsonNode data;
  private Map<String, List<FacetDTO>> facets;
  private long totalCount;
}

