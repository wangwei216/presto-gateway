package com.lyft.data.gateway.ha.router;

import lombok.Data;
import lombok.ToString;

public interface TapdbCountRuleAggregateManager {

    long getQueryCountSumByAppId(String appId);


    @Data
    @ToString
    class CountRuleAggregate {
        private String appId;
        private long createTime;
        private long queryCount;
        private long cpuTimeSum;

        private long runningTimeSum;
        private long wallTimeSum;
        private long queuedTimeSum;

        private long totalBytesSum;
        private long totalRowSum;

    }
}
