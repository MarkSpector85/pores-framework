package com.pores.framework.elasticsearch.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
public class SearchCriteria {

  private Map<String, Object> filterCriteriaMap;
  private List<RangeCriterion> rangeCriteriaList;
  private List<String> requestedFields;

  private int pageNumber;

  private int pageSize;

  private String orderBy;

  private String orderDirection;

  private String searchString;

  private List<String> facets;

  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class RangeCriterion {
    private String field;
    private Range range;
  }

  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Range {
    private Object min;
    private Object max;
  }
}
