package com.lyft.data.gateway.ha.persistence.dao;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Cached;
import org.javalite.activejdbc.annotations.DbName;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

/**
 * @Description ClusterAnalyseRouting
 * @Date 2022/8/15
 * @Author wangwei
 */
@IdName("project_id")
@Table("event_analyse_routing")
@Cached
@DbName("cluster_dispatch_db")
public class ClusterAnalyseRouting extends Model {

    private static final String projectId = "project_id";
    private static final String projectName = "project_name";
    private static final String ip = "ip";
    private static final String port = "port";
    private static final String enable = "enable";
    private static final String createdAt = "created_at";
    private static final String updatedAt = "updated_at";
    private static final String cluster = "cluster";

}
