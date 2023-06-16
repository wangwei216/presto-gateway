package com.lyft.data.gateway.ha.router;

import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;
import com.lyft.data.gateway.ha.persistence.dao.TapdbCountRuleAggregate;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.DB;


import java.math.BigDecimal;
import java.util.List;


/**
 * @Description TapdbCountRuleAggregateManager
 * @Date 2022/8/9
 * @Author wangwei
 */
@Slf4j
public class HaTapdbCountRuleAggregateManager implements TapdbCountRuleAggregateManager{

    private JdbcConnectionManager connectionManager;


    public HaTapdbCountRuleAggregateManager(JdbcConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }


    @Override
    public long getQueryCountSumByAppId(String appId) {
        try {
            connectionManager.open();
            BigDecimal queryCountSum = (BigDecimal) new DB(DB.DEFAULT_NAME).firstCell("select COALESCE(sum(query_count), 0) queryCount from tapdb_count_rule_aggregate where app_id = ? AND create_time < CURRENT_TIMESTAMP AND  create_time >= date_sub(CURRENT_TIMESTAMP, interval 5 minute) ", appId);
            return queryCountSum.longValue();
        } finally {
            connectionManager.close();
        }
    }
}
