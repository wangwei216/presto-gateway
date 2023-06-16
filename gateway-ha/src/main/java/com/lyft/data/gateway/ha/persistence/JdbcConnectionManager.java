package com.lyft.data.gateway.ha.persistence;

import com.lyft.data.gateway.ha.config.ClusterDispatchDataStoreConfiguration;
import com.lyft.data.gateway.ha.config.DataStoreConfiguration;
import com.lyft.data.gateway.ha.persistence.dao.QueryHistory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

import com.lyft.data.gateway.ha.persistence.dao.TapdbCountRuleAggregate;
import com.lyft.data.gateway.ha.persistence.dao.TapdbQueryFailureInfo;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.DB;

@Slf4j
public class JdbcConnectionManager {
  private final DataStoreConfiguration configuration;
  private final ClusterDispatchDataStoreConfiguration clusterDispatchConfiguration;
  private static final String CLUSTER_DISPATCH_DB_NAME = "cluster_dispatch_db";
  private static final String DEFAULT_DB_NAME = "default";
  private final ScheduledExecutorService executorService =
      Executors.newSingleThreadScheduledExecutor();

  public JdbcConnectionManager(DataStoreConfiguration configuration, ClusterDispatchDataStoreConfiguration clusterDispatchConfiguration) {
    this.configuration = configuration;
    this.clusterDispatchConfiguration = clusterDispatchConfiguration;
    startCleanUps();
  }

  public void open() {
    this.open(null);
  }

  public void open(@Nullable String routingGroupDatabase) {
    String jdbcUrl = configuration.getJdbcUrl();
    if (routingGroupDatabase != null) {
      jdbcUrl = jdbcUrl.substring(0, jdbcUrl.lastIndexOf('/') + 1) + routingGroupDatabase;
    }
    log.debug("Jdbc url is " + jdbcUrl);
    new DB(DEFAULT_DB_NAME).open(
            configuration.getDriver(),
            jdbcUrl,
            configuration.getUser(),
            configuration.getPassword());
    log.debug("Connection opened");
  }

  public void openClusterDispatchDB() {
    String jdbcUrl = clusterDispatchConfiguration.getJdbcUrl();

    log.debug("Jdbc url is " + jdbcUrl);
    new DB(CLUSTER_DISPATCH_DB_NAME).open(
            clusterDispatchConfiguration.getDriver(),
            jdbcUrl,
            clusterDispatchConfiguration.getUser(),
            clusterDispatchConfiguration.getPassword());
    log.debug("clusterDispatchConfiguration Connection opened");
  }

  public void close() {
    new DB(DEFAULT_DB_NAME).close();
    log.debug("Connection closed");
  }

  public void closeClusterDispatchDB() {
    new DB(CLUSTER_DISPATCH_DB_NAME).close();
    log.debug("Cluster_dispatch_db Connection closed");
  }


  private void startCleanUps() {
    executorService.scheduleWithFixedDelay(
        () -> {
          log.info("Performing query history cleanup task");
          try {
            this.open();
            int deleteQueryHistoryCount = QueryHistory.delete(
                    "created < ?", System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3));
            int deleteRuleAggregateCount = TapdbCountRuleAggregate.delete("create_time < date_sub(CURRENT_TIMESTAMP, interval 3 Day) ");
            int deleteQueryFailureInfoCount = TapdbQueryFailureInfo.delete("create_time < date_sub(CURRENT_TIMESTAMP, interval 3 Day) ");
            log.info("Delete completely history record , effect QueryHistory table rows is {}, effect TapdbCountRuleAggregate table rows is {}, effect TapdbQueryFailureInfo table rows is {}", deleteQueryHistoryCount, deleteRuleAggregateCount, deleteQueryFailureInfoCount);
          } finally {
            this.close();
          }
        },
        1,
        180,
        TimeUnit.MINUTES);
  }
}
