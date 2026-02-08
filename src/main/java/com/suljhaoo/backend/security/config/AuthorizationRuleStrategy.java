package com.suljhaoo.backend.security.config;

import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Strategy interface for applying authorization rules. Follows Strategy Pattern and Open/Closed
 * Principle - open for extension, closed for modification.
 */
public interface AuthorizationRuleStrategy {

  /**
   * Applies the authorization rule to the given path.
   *
   * @param auth The authorization configuration builder
   * @param path The endpoint path
   * @param endpointConfig The endpoint configuration
   */
  void apply(
      AuthorizeHttpRequestsConfigurer<?>.AuthorizationManagerRequestMatcherRegistry auth,
      String path,
      EndpointRoleConfig.EndpointConfig endpointConfig);
}
