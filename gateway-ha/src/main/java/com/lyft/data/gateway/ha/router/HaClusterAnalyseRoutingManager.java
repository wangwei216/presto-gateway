package com.lyft.data.gateway.ha.router;

import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;
import com.lyft.data.gateway.ha.persistence.dao.ClusterAnalyseRouting;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.LazyList;
import org.javalite.activejdbc.Model;



/**
 * @Description HaClusterAnalyseRoutingManager
 * @Date 2022/8/15
 * @Author wangwei
 */
@Slf4j
public class HaClusterAnalyseRoutingManager implements ClusterAnalyseRoutingManager{

    private JdbcConnectionManager connectionManager;


    public HaClusterAnalyseRoutingManager(JdbcConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }


    @Override
    public String findClusterRouting(String appId) {
        // 如果找不到默认先用 saas1 集群
        String clusterRouter = "saas1";
        try {
            connectionManager.openClusterDispatchDB();
            LazyList<Model> list = ClusterAnalyseRouting.findBySQL("SELECT appid, cluster from event_analyse_routing join projects on event_analyse_routing.project_id = projects.id where appid = ? ", appId);
            if (list.size() == 1) {
                clusterRouter = (String) list.get(0).get("cluster");
            } else {
                log.error("the appid is not find in event_analyse_routing table, please check, and appId is {}", appId);
            }
            return clusterRouter;
        } finally {
            connectionManager.closeClusterDispatchDB();
        }
    }


}
