package com.suljhaoo.backend.security.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * Builder for configuring Spring Security authorization rules from JSON configuration. Follows
 * Builder Pattern and Single Responsibility Principle. Encapsulates the logic for applying
 * authorization rules to Spring Security configuration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizationConfigBuilder {

  private final EndpointRoleConfig endpointRoleConfig;
  private final AuthorizationRuleStrategyFactory strategyFactory;

  /**
   * Builds and applies authorization rules from endpoint-roles.json to Spring Security
   * configuration.
   *
   * @param auth The Spring Security authorization configuration builder
   */
  public void build(
      AuthorizeHttpRequestsConfigurer<?>.AuthorizationManagerRequestMatcherRegistry auth) {
    List<EndpointRoleConfig.EndpointConfig> endpoints = endpointRoleConfig.getEndpoints();

    if (endpoints == null || endpoints.isEmpty()) {
      log.warn("No endpoints found in endpoint-roles.json. Using default configuration.");
      return;
    }

    log.info(
        "Configuring authorization for {} endpoints from endpoint-roles.json", endpoints.size());

    for (EndpointRoleConfig.EndpointConfig endpoint : endpoints) {
      String path = endpoint.getPath();
      AuthorizationRuleStrategy strategy = strategyFactory.createStrategy(endpoint);
      strategy.apply(auth, path, endpoint);
    }
  }
}
