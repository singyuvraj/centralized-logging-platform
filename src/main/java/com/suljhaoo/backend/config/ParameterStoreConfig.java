package com.suljhaoo.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;
import software.amazon.awssdk.services.ssm.model.SsmException;

/**
 * EnvironmentPostProcessor that loads JSON configuration from AWS Parameter Store.
 *
 * <p>Loads configuration from: /config/suljhaoo-backend-service/{environment}/application.json
 *
 * <p>Environment is determined from:
 *
 * <ul>
 *   <li>Property: aws.parameterstore.environment (if set)
 *   <li>Active Spring profile (first active profile, excluding 'local')
 *   <li>Default: 'dev'
 * </ul>
 *
 * <p>Uses AWS SDK default credential chain, which automatically handles:
 *
 * <ul>
 *   <li>Local: ~/.aws/credentials or environment variables
 *   <li>EC2: IAM instance role
 *   <li>ECS: Task role
 * </ul>
 */
@Slf4j
public class ParameterStoreConfig implements EnvironmentPostProcessor {

  private static final String PARAMETER_PATH_PREFIX = "/config/suljhaoo-backend-service";
  private static final String PARAMETER_NAME = "application.json";
  private static final String PROPERTY_SOURCE_NAME = "parameterStoreJsonConfig";
  private static final String ENV_PROPERTY = "aws.parameterstore.environment";
  private static final String REGION_PROPERTY = "spring.cloud.aws.region.static";
  private static final String DEFAULT_REGION = "ap-south-1";
  private static final String DEFAULT_ENV = "dev";

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {

    // Skip Parameter Store if "local" or "test" profile is active
    String[] activeProfiles = environment.getActiveProfiles();
    for (String profile : activeProfiles) {
      if ("local".equalsIgnoreCase(profile) || "test".equalsIgnoreCase(profile)) {
        String skipMessage =
            "⏭️ Skipping Parameter Store (local profile detected). Using local configuration only.";
        log.info(skipMessage);
        System.out.println("[ParameterStoreConfig] " + skipMessage);
        return;
      }
    }

    String env = determineEnvironment(environment);
    String region = environment.getProperty(REGION_PROPERTY, DEFAULT_REGION);
    String parameterPath = String.format("%s/%s/%s", PARAMETER_PATH_PREFIX, env, PARAMETER_NAME);

    // Log which environment is being used
    String envInfo =
        String.format(
            "🔍 Loading Parameter Store config for environment: %s, path: %s", env, parameterPath);
    log.info(envInfo);
    System.out.println("[ParameterStoreConfig] " + envInfo);

    try {
      Map<String, Object> properties = loadJsonFromParameterStore(parameterPath, region);

      if (!properties.isEmpty()) {
        PropertySource<?> propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, properties);
        environment.getPropertySources().addFirst(propertySource);
        // Use both logger and System.out for early lifecycle visibility
        String logMessage =
            String.format(
                "✅ Loaded %d properties from Parameter Store: %s (profile overrides will take precedence)",
                properties.size(), parameterPath);
        log.info(logMessage);
        System.out.println("[ParameterStoreConfig] " + logMessage);
      } else {
        String warnMessage =
            String.format("⚠️ No properties loaded from Parameter Store: %s", parameterPath);
        log.warn(warnMessage);
        System.out.println("[ParameterStoreConfig] " + warnMessage);
      }
    } catch (Exception e) {
      String errorMessage =
          String.format(
              "❌ Failed to load configuration from Parameter Store (%s): %s. "
                  + "Application will continue with local configuration.",
              parameterPath, e.getMessage());
      log.warn(errorMessage, e);
      System.out.println("[ParameterStoreConfig] " + errorMessage);
      log.debug("Parameter Store load error details", e);
    }
  }

  private String determineEnvironment(ConfigurableEnvironment environment) {
    // Check explicit property first (can be set via environment variable
    // AWS_PARAMETERSTORE_ENVIRONMENT
    // or system property aws.parameterstore.environment)
    String explicitEnv = environment.getProperty(ENV_PROPERTY);
    if (explicitEnv != null && !explicitEnv.trim().isEmpty()) {
      log.debug("Using Parameter Store environment from property: {}", explicitEnv);
      return explicitEnv.trim();
    }

    // Also check environment variable directly (Spring Boot converts env vars, but check both
    // formats)
    explicitEnv = System.getenv("AWS_PARAMETERSTORE_ENVIRONMENT");
    if (explicitEnv != null && !explicitEnv.trim().isEmpty()) {
      log.debug("Using Parameter Store environment from environment variable: {}", explicitEnv);
      return explicitEnv.trim();
    }

    // Use first active profile (excluding 'local' and 'test')
    String[] activeProfiles = environment.getActiveProfiles();
    for (String profile : activeProfiles) {
      if (!"local".equals(profile) && !"test".equals(profile)) {
        log.debug("Using Parameter Store environment from active profile: {}", profile);
        return profile;
      }
    }

    // Default to dev
    log.debug("Using default Parameter Store environment: {}", DEFAULT_ENV);
    return DEFAULT_ENV;
  }

  private Map<String, Object> loadJsonFromParameterStore(String parameterPath, String region) {
    Map<String, Object> properties = new HashMap<>();

    try (SsmClient ssmClient =
        SsmClient.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .build()) {

      GetParameterRequest request =
          GetParameterRequest.builder().name(parameterPath).withDecryption(true).build();

      String jsonValue = ssmClient.getParameter(request).parameter().value();

      if (jsonValue != null && !jsonValue.trim().isEmpty()) {
        ObjectMapper objectMapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> jsonMap = objectMapper.readValue(jsonValue, Map.class);

        flattenMap("", jsonMap, properties);
        log.debug("Parsed {} properties from JSON", properties.size());
      }

    } catch (ParameterNotFoundException e) {
      log.debug("Parameter not found: {}", parameterPath);
    } catch (SsmException e) {
      log.debug("AWS SSM error loading parameter {}: {}", parameterPath, e.getMessage());
      throw new RuntimeException("Failed to load parameter from Parameter Store", e);
    } catch (Exception e) {
      log.debug("Error parsing JSON from parameter {}: {}", parameterPath, e.getMessage());
      throw new RuntimeException("Failed to parse JSON from Parameter Store", e);
    }

    return properties;
  }

  @SuppressWarnings("unchecked")
  private void flattenMap(String prefix, Map<String, Object> source, Map<String, Object> target) {
    for (Map.Entry<String, Object> entry : source.entrySet()) {
      String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
      Object value = entry.getValue();

      if (value instanceof Map) {
        flattenMap(key, (Map<String, Object>) value, target);
      } else {
        target.put(key, value);
      }
    }
  }
}
