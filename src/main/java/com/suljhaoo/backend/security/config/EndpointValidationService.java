package com.suljhaoo.backend.security.config;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service that validates all controller endpoints are defined in endpoint-roles.json. Follows
 * Single Responsibility Principle - only responsible for endpoint validation. Fails application
 * startup if strict validation is enabled and endpoints don't match.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EndpointValidationService {

  private final ApplicationContext applicationContext;
  private final EndpointRoleConfig endpointRoleConfig;

  @PostConstruct
  public void validateEndpoints() {
    // Config is already loaded by EndpointRoleConfig's @PostConstruct
    // This runs after EndpointRoleConfig.init() completes
    if (!endpointRoleConfig.isStrictValidation()) {
      log.warn("Strict validation is disabled. Skipping endpoint validation.");
      return;
    }

    log.info("Starting endpoint validation...");
    Set<EndpointSignature> controllerEndpoints = scanControllerEndpoints();
    Set<EndpointSignature> configEndpoints = getConfigEndpoints();

    // Filter out actuator endpoints from config endpoints for validation
    // Actuator endpoints are not @RestController endpoints, so they shouldn't be validated
    Set<EndpointSignature> configEndpointsForValidation =
        configEndpoints.stream()
            .filter(endpoint -> !isActuatorEndpoint(endpoint))
            .collect(Collectors.toSet());

    List<String> missingInConfig = findMissing(controllerEndpoints, configEndpointsForValidation);
    List<String> extraInConfig = findMissing(configEndpointsForValidation, controllerEndpoints);

    if (!missingInConfig.isEmpty() || !extraInConfig.isEmpty()) {
      String errorMessage = constructErrorDetails(missingInConfig, extraInConfig);
      log.error(errorMessage);

      // Create a more descriptive exception message
      String exceptionMessage =
          String.format(
              """
                          Endpoint validation failed. Server cannot start.
                          Missing endpoints: %d | Extra endpoints: %d
                          See logs above for detailed endpoint information.""",
              missingInConfig.size(), extraInConfig.size());

      throw new IllegalStateException(exceptionMessage);
    }

    log.info(
        "Endpoint validation passed. All {} endpoints are properly configured.",
        controllerEndpoints.size());
  }

  private String constructErrorDetails(List<String> missingInConfig, List<String> extraInConfig) {
    StringBuilder error = new StringBuilder("\n");
    error.append(
        "╔══════════════════════════════════════════════════════════════════════════════╗\n");
    error.append(
        "║                    ENDPOINT VALIDATION FAILED                                ║\n");
    error.append(
        "║              Server cannot start due to endpoint mismatch                   ║\n");
    error.append(
        "╚══════════════════════════════════════════════════════════════════════════════╝\n\n");

    if (!missingInConfig.isEmpty()) {
      error.append("❌ MISSING ENDPOINTS (Found in controllers but NOT in endpoint-roles.json):\n");
      error.append("   These endpoints need to be added to endpoint-roles.json:\n\n");
      for (int i = 0; i < missingInConfig.size(); i++) {
        error.append(String.format("   %d. %s\n", i + 1, missingInConfig.get(i)));
      }
      error.append("\n");
    }

    if (!extraInConfig.isEmpty()) {
      error.append("⚠️  EXTRA ENDPOINTS (In endpoint-roles.json but NOT found in controllers):\n");
      error.append("   These endpoints should be removed from endpoint-roles.json:\n\n");
      for (int i = 0; i < extraInConfig.size(); i++) {
        error.append(String.format("   %d. %s\n", i + 1, extraInConfig.get(i)));
      }
      error.append("\n");
    }

    error.append(
        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
    error.append("📝 SOLUTION:\n");
    error.append("   1. Update endpoint-roles.json to include all missing endpoints\n");
    error.append("   2. Remove or fix any extra endpoints listed above\n");
    error.append(
        "   3. Or set 'strictValidation: false' in endpoint-roles.json to disable this check\n");
    error.append(
        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

    return error.toString();
  }

  private Set<EndpointSignature> scanControllerEndpoints() {
    Set<EndpointSignature> endpoints = new HashSet<>();

    applicationContext
        .getBeansWithAnnotation(RestController.class)
        .values()
        .forEach(
            bean -> {
              // Get the target class (handles AOP proxies)
              Class<?> beanClass = AopProxyUtils.ultimateTargetClass(bean);
              RequestMapping classMapping = beanClass.getAnnotation(RequestMapping.class);
              String basePath =
                  classMapping != null && classMapping.value().length > 0
                      ? normalizePathVariables(classMapping.value()[0])
                      : "";

              // Use getMethods() to include all public methods (works with AOP proxies)
              for (Method method : beanClass.getMethods()) {
                // Only process methods declared in the actual controller class (not Object methods)
                if (method.getDeclaringClass() == Object.class) {
                  continue;
                }
                String httpMethod = getHttpMethod(method);
                if (httpMethod != null) {
                  String methodPath = getMethodPath(method);
                  String fullPath = combinePaths(basePath, methodPath);
                  endpoints.add(new EndpointSignature(fullPath, httpMethod));
                }
              }
            });

    return endpoints;
  }

  private Set<EndpointSignature> getConfigEndpoints() {
    return endpointRoleConfig.getEndpoints().stream()
        .map(
            config -> {
              String method = "*".equals(config.getMethod()) ? "*" : config.getMethod();
              String normalizedPath = normalizePathVariables(config.getPath());
              return new EndpointSignature(normalizedPath, method);
            })
        .collect(Collectors.toSet());
  }

  private String getHttpMethod(Method method) {
    if (method.isAnnotationPresent(GetMapping.class)) return "GET";
    if (method.isAnnotationPresent(PostMapping.class)) return "POST";
    if (method.isAnnotationPresent(PutMapping.class)) return "PUT";
    if (method.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
    if (method.isAnnotationPresent(PatchMapping.class)) return "PATCH";
    return null;
  }

  private String getMethodPath(Method method) {
    GetMapping getMapping = method.getAnnotation(GetMapping.class);
    if (getMapping != null && getMapping.value().length > 0) {
      return getMapping.value()[0];
    }

    PostMapping postMapping = method.getAnnotation(PostMapping.class);
    if (postMapping != null && postMapping.value().length > 0) {
      return postMapping.value()[0];
    }

    PutMapping putMapping = method.getAnnotation(PutMapping.class);
    if (putMapping != null && putMapping.value().length > 0) {
      return putMapping.value()[0];
    }

    DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
    if (deleteMapping != null && deleteMapping.value().length > 0) {
      return deleteMapping.value()[0];
    }

    PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
    if (patchMapping != null && patchMapping.value().length > 0) {
      return patchMapping.value()[0];
    }

    RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
    if (requestMapping != null && requestMapping.value().length > 0) {
      return requestMapping.value()[0];
    }

    return "";
  }

  private String combinePaths(String basePath, String methodPath) {
    if (basePath == null) basePath = "";
    if (methodPath == null) methodPath = "";

    if (!basePath.startsWith("/")) basePath = "/" + basePath;
    if (basePath.endsWith("/")) basePath = basePath.substring(0, basePath.length() - 1);

    if (methodPath.startsWith("/")) methodPath = methodPath.substring(1);

    String combined;
    if (basePath.equals("/")) {
      combined = "/" + methodPath;
    } else if (methodPath.isEmpty()) {
      combined = basePath;
    } else {
      combined = basePath + "/" + methodPath;
    }

    return normalizePathVariables(combined);
  }

  private String normalizePathVariables(String path) {
    if (path == null || path.isEmpty()) {
      return "/";
    }
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    return path.replaceAll("\\{[^}]+}", "{id}");
  }

  private List<String> findMissing(Set<EndpointSignature> source, Set<EndpointSignature> target) {
    List<String> missing = new ArrayList<>();
    for (EndpointSignature sourceEndpoint : source) {
      boolean found = false;
      for (EndpointSignature targetEndpoint : target) {
        if (matches(sourceEndpoint, targetEndpoint)) {
          found = true;
          break;
        }
      }
      if (!found) {
        missing.add(sourceEndpoint.toString());
      }
    }
    return missing;
  }

  /**
   * Checks if an endpoint is an actuator endpoint (not a @RestController endpoint). Actuator
   * endpoints should be excluded from validation as they are not @RestController endpoints.
   */
  private boolean isActuatorEndpoint(EndpointSignature endpoint) {
    return endpoint.path().startsWith("/actuator/");
  }

  private boolean matches(EndpointSignature source, EndpointSignature target) {
    boolean pathMatches = false;

    if (source.path.equals(target.path)) {
      pathMatches = true;
    } else if (target.path.endsWith("/**")) {
      String basePath = target.path.substring(0, target.path.length() - 3);
      if (basePath.isEmpty()) {
        basePath = "/";
      }
      pathMatches = source.path.startsWith(basePath);
    } else if (source.path.endsWith("/**")) {
      String basePath = source.path.substring(0, source.path.length() - 3);
      if (basePath.isEmpty()) {
        basePath = "/";
      }
      pathMatches = target.path.startsWith(basePath);
    }

    boolean methodMatches =
        "*".equals(target.method)
            || "*".equals(source.method)
            || source.method.equals(target.method);

    return pathMatches && methodMatches;
  }

  private record EndpointSignature(String path, String method) {
    @Override
    public String toString() {
      return method + " " + path;
    }
  }
}
