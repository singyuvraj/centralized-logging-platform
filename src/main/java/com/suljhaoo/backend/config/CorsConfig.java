package com.suljhaoo.backend.config;

import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Slf4j
@Configuration
public class CorsConfig {

  @Value(
      "${cors.origin:http://localhost:3000,http://localhost:3001,https://dev.suljhaoo.com,https://staging.suljhaoo.com,https://suljhaoo.com}")
  private String allowedOrigins;

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // Parse allowed origins from comma-separated string
    List<String> origins = Arrays.asList(allowedOrigins.split(","));
    List<String> trimmedOrigins = origins.stream().map(String::trim).toList();
    configuration.setAllowedOrigins(trimmedOrigins);

    // Log configured origins for debugging
    log.info("CORS Configuration - Allowed Origins: {}", trimmedOrigins);

    // Allow common HTTP methods
    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

    // Allow common headers
    configuration.setAllowedHeaders(List.of("*"));

    // Allow credentials (cookies, authorization headers)
    configuration.setAllowCredentials(true);

    // Expose headers that the client might need
    configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));

    // Cache preflight requests for 1 hour
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    log.info("CORS Configuration - Allowed Methods: {}", configuration.getAllowedMethods());
    log.info("CORS Configuration - Allow Credentials: {}", configuration.getAllowCredentials());
    log.info("CORS Configuration - Max Age: {}", configuration.getMaxAge());

    return source;
  }
}
