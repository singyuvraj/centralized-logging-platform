package com.suljhaoo.backend.security.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Factory for creating appropriate authorization rule strategies. Follows Factory Pattern and
 * Dependency Inversion Principle.
 */
@Component
@RequiredArgsConstructor
public class AuthorizationRuleStrategyFactory {

  private final PublicEndpointStrategy publicEndpointStrategy;
  private final SingleRoleStrategy singleRoleStrategy;
  private final MultipleRolesStrategy multipleRolesStrategy;

  /**
   * Creates the appropriate strategy based on endpoint configuration.
   *
   * @param endpointConfig The endpoint configuration
   * @return The appropriate authorization rule strategy
   */
  public AuthorizationRuleStrategy createStrategy(
      EndpointRoleConfig.EndpointConfig endpointConfig) {
    if (endpointConfig.isPublic()) {
      return publicEndpointStrategy;
    }

    List<String> roles = endpointConfig.getRoles();
    if (roles.size() == 1) {
      return singleRoleStrategy;
    }

    return multipleRolesStrategy;
  }
}
