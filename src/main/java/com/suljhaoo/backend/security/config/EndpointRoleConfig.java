package com.suljhaoo.backend.security.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Configuration class that loads endpoint-role mappings from JSON file. Follows Single
 * Responsibility Principle - only responsible for loading configuration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EndpointRoleConfig {

  @Value("classpath:endpoint-roles.json")
  private Resource resource;

  private final ObjectMapper objectMapper;

  @Getter private EndpointRolesConfig config;

  @PostConstruct
  public void init() throws IOException {
    // Validate that the JSON file exists and is accessible
    validateJsonFile();
    // Load configuration
    loadConfig();
  }

  /**
   * Validates that the endpoint-roles.json file exists and is accessible. Throws an exception if
   * the file is missing or cannot be read.
   */
  private void validateJsonFile() throws IOException {
    if (resource == null) {
      throw new IllegalStateException(
          "endpoint-roles.json resource is null. The file must exist in classpath.");
    }

    if (!resource.exists()) {
      throw new IllegalStateException(
          "endpoint-roles.json file does not exist in classpath. "
              + "This file is mandatory for endpoint role configuration.");
    }

    if (!resource.isReadable()) {
      throw new IllegalStateException(
          "endpoint-roles.json file exists but is not readable. "
              + "Please check file permissions.");
    }

    log.debug("Validated endpoint-roles.json file exists and is readable");
  }

  public void loadConfig() throws IOException {
    try (InputStream inputStream = resource.getInputStream()) {
      this.config = objectMapper.readValue(inputStream, EndpointRolesConfig.class);
      log.info(
          "Loaded {} endpoint-role mappings from {}",
          config.getEndpoints().size(),
          resource.getFilename());
    } catch (IOException e) {
      log.error("Failed to load endpoint-roles.json", e);
      throw new IOException("Failed to load endpoint-roles.json: " + e.getMessage(), e);
    }
  }

  public List<EndpointConfig> getEndpoints() {
    return config != null ? config.getEndpoints() : new ArrayList<>();
  }

  public boolean isStrictValidation() {
    return config != null && config.isStrictValidation();
  }

  @Data
  public static class EndpointRolesConfig {
    private List<EndpointConfig> endpoints;
    private String defaultRole;
    private boolean strictValidation;
  }

  @Data
  public static class EndpointConfig {
    private String path;
    private String method;

    @JsonProperty("roles")
    private List<String> roles;

    private String description;

    public boolean isPublic() {
      return roles == null || roles.isEmpty();
    }
  }
}
