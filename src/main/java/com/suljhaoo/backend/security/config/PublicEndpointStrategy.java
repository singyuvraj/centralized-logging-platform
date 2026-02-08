package com.suljhaoo.backend.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * Strategy for public endpoints (no authentication required). Follows Single Responsibility
 * Principle.
 */
@Slf4j
@Component
public class PublicEndpointStrategy implements AuthorizationRuleStrategy {

  @Override
  public void apply(
      AuthorizeHttpRequestsConfigurer<?>.AuthorizationManagerRequestMatcherRegistry auth,
      String path,
      EndpointRoleConfig.EndpointConfig endpointConfig) {
    auth.requestMatchers(path).permitAll();
    log.debug("Configured public endpoint: {}", path);
  }
}
