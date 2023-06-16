package com.lyft.data.gateway.ha.router;

import lombok.Data;
import lombok.ToString;

public interface TapdbQueryFailureInfoManager {
  boolean isHitErrorByQuerySqlMd5(String appId, String querySql);

  @Data
  @ToString
  class QueryFailureInfo {
    private String appId;
    private long createTime;
    private String queryId;
    private String query;
    private String queryMd5;
    private String errorName;
    private String errorType;
    private String failureType;
    private String failureMessage;
    private String failuresJson;
    private String prepareQuery;
  }

}
