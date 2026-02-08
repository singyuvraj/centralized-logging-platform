package com.suljhaoo.backend.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/** Strategy for endpoints requiring a single role. Follows Single Responsibility Principle. */
@Slf4j
@Component
public class SingleRoleStrategy implements AuthorizationRuleStrategy {

  @Override
  public void apply(
      AuthorizeHttpRequestsConfigurer<?>.AuthorizationManagerRequestMatcherRegistry auth,
      String path,
      EndpointRoleConfig.EndpointConfig endpointConfig) {
    String role = endpointConfig.getRoles().get(0);
    auth.requestMatchers(path).hasRole(role);
    log.debug("Configured endpoint {} with role: {}", path, role);
  }
}
