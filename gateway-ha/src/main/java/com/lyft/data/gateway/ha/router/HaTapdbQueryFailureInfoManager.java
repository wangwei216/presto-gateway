package com.lyft.data.gateway.ha.router;

import com.lyft.data.gateway.ha.persistence.JdbcConnectionManager;
import com.lyft.data.gateway.ha.persistence.dao.TapdbQueryFailureInfo;
import com.lyft.data.gateway.ha.uitl.MD5Util;
import java.util.List;

/**
 * @Description HaTapdbQueryFailureInfoManager
 * @Date 2022/8/15
 * @Author wangwei
 */
public class HaTapdbQueryFailureInfoManager implements TapdbQueryFailureInfoManager {

    public static final String INSUFFICIENT_RESOURCES = "INSUFFICIENT_RESOURCES";
    public static final String EXCEEDED_TIME_LIMIT = "EXCEEDED_TIME_LIMIT";

    private JdbcConnectionManager connectionManager;


    public HaTapdbQueryFailureInfoManager(JdbcConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }


    @Override
    public boolean isHitErrorByQuerySqlMd5(String appId, String querySql) {
        try {
            // TODO 这里根据 sql 生成的 MD5 是动态变化的，目前不能命中
            String querySqlMD5 = MD5Util.encodeMD5(querySql);
            connectionManager.open();
            List<TapdbQueryFailureInfo> list = TapdbQueryFailureInfo.findBySQL("SELECT * FROM tapdb_query_failure_info where app_id = ? AND query_md5 = ? AND error_type = ? AND error_name != ? AND create_time >= date_sub(CURRENT_TIMESTAMP, interval 20 minute) ", appId, querySqlMD5, INSUFFICIENT_RESOURCES, EXCEEDED_TIME_LIMIT);
            return list.isEmpty();
        } finally {
            connectionManager.close();
        }
    }
}
