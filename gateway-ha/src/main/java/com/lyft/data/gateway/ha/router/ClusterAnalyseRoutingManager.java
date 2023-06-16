package com.lyft.data.gateway.ha.router;

import lombok.Data;
import lombok.ToString;

public interface ClusterAnalyseRoutingManager {

    String findClusterRouting(String appId);

    @Data
    @ToString
    class ClusterAnalyseRouter {
        private long projectId;
        private String projectName;
        private String ip;
        private int port;
        private boolean enable;
        private long createdAt;
        private long updatedAt;
        private String cluster;
    }

}
