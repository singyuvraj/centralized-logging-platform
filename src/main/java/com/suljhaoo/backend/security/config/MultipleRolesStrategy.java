package com.suljhaoo.backend.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * Strategy for endpoints requiring any of multiple roles. Follows Single Responsibility Principle.
 */
@Slf4j
@Component
public class MultipleRolesStrategy implements AuthorizationRuleStrategy {

  @Override
  public void apply(
      AuthorizeHttpRequestsConfigurer<?>.AuthorizationManagerRequestMatcherRegistry auth,
      String path,
      EndpointRoleConfig.EndpointConfig endpointConfig) {
    String[] roleArray = endpointConfig.getRoles().toArray(new String[0]);
    auth.requestMatchers(path).hasAnyRole(roleArray);
    log.debug("Configured endpoint {} with roles: {}", path, endpointConfig.getRoles());
  }
}
