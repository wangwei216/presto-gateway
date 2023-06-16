package com.lyft.data.gateway.ha.persistence.dao;

import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Cached;
import org.javalite.activejdbc.annotations.CompositePK;
import org.javalite.activejdbc.annotations.DbName;
import org.javalite.activejdbc.annotations.Table;

/**
 * @Description TapdbQueryFailureInfo
 * @Date 2022/8/10
 * @Author wangwei
 */
@Slf4j
@Table("tapdb_query_failure_info")
@CompositePK({"app_id", "create_time"})
@Cached
@DbName("default")
public class TapdbQueryFailureInfo extends Model {

    private static final String appId = "app_id";
    private static final String createTime = "create_time";
    private static final String queryId = "query_id";
    private static final String query = "query";
    private static final String prepareQuery = "prepare_query";
    private static final String queryMd5 = "query_md5";
    private static final String errorName = "error_name";
    private static final String errorType = "error_type";
    private static final String failureType = "failure_type";
    private static final String failureMessage = "failure_message";
    private static final String failuresJson = "failures_json";

}
