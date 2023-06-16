package com.lyft.data.gateway.ha.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Description ClusterDispatchDataStoreConfiguration
 * @Date 2022/8/12
 * @Author wangwei
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClusterDispatchDataStoreConfiguration {
    private String jdbcUrl;
    private String user;
    private String password;
    private String driver;
}
