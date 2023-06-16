package com.lyft.data.gateway.ha.persistence.dao;

import com.lyft.data.gateway.ha.router.TapdbCountRuleAggregateManager;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Cached;
import org.javalite.activejdbc.annotations.CompositePK;
import org.javalite.activejdbc.annotations.DbName;
import org.javalite.activejdbc.annotations.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description TapdbCountRuleAggregate
 * @Date 2022/8/9
 * @Author wangwei
 */
@Slf4j
@Table("tapdb_count_rule_aggregate")
@CompositePK({"app_id", "create_time"})
@Cached
@DbName("default")
public class TapdbCountRuleAggregate extends Model {

    private static final String appId = "app_id";
    private static final String createTime = "create_time";
    private static final String queryCount = "query_count";
    private static final String cpuTimeSum = "cpu_time_sum";
    private static final String runningTimeSum = "running_time_sum";
    private static final String wallTimeSum = "wall_time_sum";
    private static final String queuedTimeSum = "queued_time_sum";
    private static final String totalBytesSum = "total_bytes_sum";
    private static final String totalRowSum = "total_row_sum";


    public static List<TapdbCountRuleAggregateManager.CountRuleAggregate> upcast(List<TapdbCountRuleAggregate> tapdbCountRuleAggregateList) {
        List<TapdbCountRuleAggregateManager.CountRuleAggregate> countRuleAggregateList = new ArrayList<>();

        for (TapdbCountRuleAggregate dao : tapdbCountRuleAggregateList) {
            TapdbCountRuleAggregateManager.CountRuleAggregate countRuleAggregate = new TapdbCountRuleAggregateManager.CountRuleAggregate();
            countRuleAggregate.setAppId(dao.getString(appId));
            countRuleAggregate.setCreateTime(dao.getLong(createTime));
            countRuleAggregate.setQueryCount(dao.getLong(queryCount));
            countRuleAggregate.setCpuTimeSum(dao.getLong(cpuTimeSum));
            countRuleAggregate.setRunningTimeSum(dao.getLong(runningTimeSum));
            countRuleAggregate.setWallTimeSum(dao.getLong(wallTimeSum));
            countRuleAggregate.setQueuedTimeSum(dao.getLong(queuedTimeSum));
            countRuleAggregate.setTotalBytesSum(dao.getLong(totalBytesSum));
            countRuleAggregate.setTotalRowSum(dao.getLong(totalRowSum));
            countRuleAggregateList.add(countRuleAggregate);
        }
        return countRuleAggregateList;
    }

    public static void create(TapdbCountRuleAggregate model, TapdbCountRuleAggregateManager.CountRuleAggregate countRuleAggregate) {
        model.set(appId, countRuleAggregate.getAppId());
        model.set(createTime, countRuleAggregate.getCreateTime());
        model.set(queryCount, countRuleAggregate.getQueryCount());
        model.set(cpuTimeSum, countRuleAggregate.getCpuTimeSum());
        model.set(runningTimeSum, countRuleAggregate.getRunningTimeSum());
        model.set(wallTimeSum, countRuleAggregate.getWallTimeSum());
        model.set(queuedTimeSum, countRuleAggregate.getQueuedTimeSum());
        model.set(totalBytesSum, countRuleAggregate.getTotalBytesSum());
        model.set(totalRowSum, countRuleAggregate.getTotalRowSum());
        model.insert();
    }

}
