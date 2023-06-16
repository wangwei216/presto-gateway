package com.lyft.data.gateway.ha.clustermonitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.lyft.data.gateway.ha.config.MonitorConfiguration;
import com.lyft.data.gateway.ha.config.ProxyBackendConfiguration;
import com.lyft.data.gateway.ha.router.GatewayBackendManager;
import io.dropwizard.lifecycle.Managed;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;

import static com.lyft.data.gateway.ha.handler.QueryIdCachingProxyHandler.*;

@Slf4j
public class ActiveClusterMonitor implements Managed {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  public static final int BACKEND_CONNECT_TIMEOUT_SECONDS = 15;
  public static final int MONITOR_TASK_DELAY_MIN = 5;
  public static final int DEFAULT_THREAD_POOL_SIZE = 10;
  public static final String PRESTO_UI_TOKEN = "Presto-UI-Token";
  public static final String CONTENT_TYPE = "application/x-www-form-urlencoded";
  public static final Map<String, ClusterStats> clusterStatsInfoMap = new HashMap<>();

  private final List<PrestoClusterStatsObserver> clusterStatsObservers;
  private final GatewayBackendManager gatewayBackendManager;
  private final int connectionTimeout;
  private final int taskDelayMin;

  private volatile boolean monitorActive = true;

  private ExecutorService executorService = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE);
  private ExecutorService singleTaskExecutor = Executors.newSingleThreadExecutor();

  /**
   * 获取集群实时状态的信息
   * @return
   */
  public static Map<String, ClusterStats> getClusterStatsInfo() {
    return clusterStatsInfoMap;
  }

  @Inject
  public ActiveClusterMonitor(
      List<PrestoClusterStatsObserver> clusterStatsObservers,
      GatewayBackendManager gatewayBackendManager,
      MonitorConfiguration monitorConfiguration) {
    this.clusterStatsObservers = clusterStatsObservers;
    this.gatewayBackendManager = gatewayBackendManager;
    this.connectionTimeout = monitorConfiguration.getConnectionTimeout();
    this.taskDelayMin = monitorConfiguration.getTaskDelayMin();
    log.info("Running cluster monitor with connection timeout of {} and task delay of {}",
        connectionTimeout, taskDelayMin);
  }

  /**
   * Run an app that queries all active presto clusters for stats.
   */
  public void start() {
    singleTaskExecutor.submit(
        () -> {
          while (monitorActive) {
            try {
              List<ProxyBackendConfiguration> activeClusters =
                  gatewayBackendManager.getAllActiveBackends();
              List<Future<ClusterStats>> futures = new ArrayList<>();
              for (ProxyBackendConfiguration backend : activeClusters) {
                Future<ClusterStats> call =
                    executorService.submit(() -> getPrestoClusterStats(backend));
                futures.add(call);
              }
              List<ClusterStats> stats = new ArrayList<>();
              for (Future<ClusterStats> clusterStatsFuture : futures) {
                ClusterStats clusterStats = clusterStatsFuture.get();
                clusterStatsInfoMap.put(clusterStats.getClusterId(), clusterStats);
                stats.add(clusterStats);
              }

              if (clusterStatsObservers != null) {
                for (PrestoClusterStatsObserver observer : clusterStatsObservers) {
                  observer.observe(stats);
                }
              }

            } catch (Exception e) {
              log.error("Error performing backend monitor tasks", e);
            }
            try {
              Thread.sleep(TimeUnit.SECONDS.toMillis(taskDelayMin));
            } catch (Exception e) {
              log.error("Error with monitor task", e);
            }
          }
        });
  }

  public String prestoLogin(ProxyBackendConfiguration backend) {
    String headerCookie = null;
    String target = backend.getProxyTo() + UI_LOGIN_PATH;
    HttpURLConnection conn = null;
    try {
      CookieManager cookieManager = new CookieManager();
      CookieHandler.setDefault(cookieManager);
      cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
      URL url = new URL(target);
      String queryBody = "username=presto-gateway&password=root";
      byte[] postDataBytes =queryBody.getBytes("UTF-8");
      conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod(HttpMethod.POST);
      conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE);
      conn.setRequestProperty(HttpHeaders.CONTENT_LENGTH, String.valueOf(postDataBytes.length));
      conn.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(connectionTimeout));
      conn.setReadTimeout((int) TimeUnit.SECONDS.toMillis(connectionTimeout));
      conn.setDoOutput(true);
      conn.getOutputStream().write(postDataBytes);
      conn.connect();
      cookieManager.put(conn.getURL().toURI(), conn.getHeaderFields());
      int responseCode = conn.getResponseCode();
      if (responseCode == HttpStatus.SC_OK) {
        List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
        if (!cookies.isEmpty()) {
          headerCookie = cookies.get(0).getName().equals(PRESTO_UI_TOKEN) ? cookies.get(0).getValue() : null;
        }
      } else {
        log.warn("Received non 200 response, response code: {}", responseCode);
      }
    } catch (Exception e) {
      log.error("Error fetching cluster stats from [{}]", target, e);
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
    return headerCookie;
  }


  private ClusterStats getPrestoClusterStats(ProxyBackendConfiguration backend) {
    ClusterStats clusterStats = new ClusterStats();
    clusterStats.setClusterId(backend.getName());
    String target = backend.getProxyTo() + UI_API_STATS_PATH;
    HttpURLConnection conn = null;
    String headerCookie = prestoLogin(backend);
    try {
      URL url = new URL(target);
      conn = (HttpURLConnection) url.openConnection();
      conn.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(connectionTimeout));
      conn.setReadTimeout((int) TimeUnit.SECONDS.toMillis(connectionTimeout));
      conn.setRequestMethod(HttpMethod.GET);

      Map<String, String> headers = new HashMap<>();
      headers.put(PRESTO_UI_TOKEN, headerCookie);
      for (String headerKey : headers.keySet()) {
        conn.setRequestProperty(headerKey, headers.get(headerKey));
      }
      conn.connect();
      int responseCode = conn.getResponseCode();
      if (responseCode == HttpStatus.SC_OK) {
        clusterStats.setHealthy(true);
        BufferedReader reader =
            new BufferedReader(new InputStreamReader((InputStream) conn.getContent()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
          sb.append(line + "\n");
        }
        HashMap<String, Object> result = OBJECT_MAPPER.readValue(sb.toString(), HashMap.class);
        clusterStats.setNumWorkerNodes((int) result.get("activeWorkers"));
        clusterStats.setQueuedQueryCount((int) result.get("queuedQueries"));
        clusterStats.setRunningQueryCount((int) result.get("runningQueries"));
        clusterStats.setBlockedQueryCount((int) result.get("blockedQueries"));
        clusterStats.setProxyTo(backend.getProxyTo());
        clusterStats.setExternalUrl(backend.getExternalUrl());
        clusterStats.setRoutingGroup(backend.getRoutingGroup());
      } else {
        log.warn("Received non 200 response, response code: {}", responseCode);
      }
    } catch (Exception e) {
      log.error("Error fetching cluster stats from [{}]", target, e);
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
    return clusterStats;
  }

  /**
   * Shut down the app.
   */
  public void stop() {
    this.monitorActive = false;
    this.executorService.shutdown();
    this.singleTaskExecutor.shutdown();
  }

}
