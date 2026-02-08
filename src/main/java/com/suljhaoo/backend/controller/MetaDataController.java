package com.suljhaoo.backend.controller;

import com.suljhaoo.backend.model.response.HealthResponse;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetaDataController {

  @Value("${spring.application.name}")
  private String appName;

  @Value("${spring.application.version}")
  private String version;

  @Autowired(required = false)
  private DataSource dataSource;

  @GetMapping("/health")
  public HealthResponse health() {
    return HealthResponse.builder()
        .status(HttpStatus.OK.name())
        .appName(appName)
        .version(version)
        .message("Server is running")
        .build();
  }

  @GetMapping("/health/pool")
  public ResponseEntity<Map<String, Object>> getConnectionPoolStats() {
    Map<String, Object> stats = new HashMap<>();

    if (dataSource instanceof HikariDataSource) {
      HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
      HikariPoolMXBean poolBean = hikariDataSource.getHikariPoolMXBean();

      stats.put("poolName", hikariDataSource.getPoolName());
      stats.put("maximumPoolSize", hikariDataSource.getMaximumPoolSize());
      stats.put("minimumIdle", hikariDataSource.getMinimumIdle());
      stats.put("activeConnections", poolBean.getActiveConnections());
      stats.put("idleConnections", poolBean.getIdleConnections());
      stats.put("totalConnections", poolBean.getTotalConnections());
      stats.put("threadsAwaitingConnection", poolBean.getThreadsAwaitingConnection());
      stats.put("connectionTimeout", hikariDataSource.getConnectionTimeout());
      stats.put("idleTimeout", hikariDataSource.getIdleTimeout());
      stats.put("maxLifetime", hikariDataSource.getMaxLifetime());
    } else {
      stats.put("error", "HikariCP DataSource not found");
    }

    return ResponseEntity.ok(stats);
  }
}
