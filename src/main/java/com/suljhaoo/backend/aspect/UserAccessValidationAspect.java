package com.suljhaoo.backend.aspect;

import com.suljhaoo.backend.util.SecurityUtil;
import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * AOP Aspect to validate user access by ensuring the authenticated user matches the userId
 * parameter in the method.
 *
 * <p>This aspect intercepts methods annotated with {@link ValidateUserAccess} and automatically
 * validates that the current authenticated user matches the userId from the method parameter.
 */
@Aspect
@Component
@Slf4j
@Order(1) // Execute before other aspects
public class UserAccessValidationAspect {

  private final ParameterNameDiscoverer parameterNameDiscoverer =
      new DefaultParameterNameDiscoverer();

  /**
   * Intercepts methods annotated with @ValidateUserAccess and validates user access.
   *
   * @param joinPoint the join point representing the intercepted method
   * @param validateUserAccess the annotation instance
   * @return the result of the method execution
   * @throws Throwable if validation fails or method execution fails
   */
  @Around("@annotation(validateUserAccess)")
  public Object validateUserAccess(
      ProceedingJoinPoint joinPoint, ValidateUserAccess validateUserAccess) throws Throwable {

    // Get the method signature
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();

    // Get the parameter name from annotation (defaults to "userId")
    String paramName = validateUserAccess.value();

    // Get method parameters and arguments
    String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
    Object[] args = joinPoint.getArgs();

    if (parameterNames == null) {
      log.warn(
          "Could not discover parameter names for method {}.{} - skipping user access validation",
          method.getDeclaringClass().getSimpleName(),
          method.getName());
      return joinPoint.proceed();
    }

    // Find the userId parameter value
    String userIdFromParam = null;
    for (int i = 0; i < parameterNames.length; i++) {
      if (parameterNames[i].equals(paramName)) {
        if (args[i] != null) {
          userIdFromParam = args[i].toString();
        }
        break;
      }
    }

    // If parameter not found, log warning and proceed (fail-safe)
    if (userIdFromParam == null) {
      log.warn(
          "Parameter '{}' not found in method {}.{} - skipping user access validation",
          paramName,
          method.getDeclaringClass().getSimpleName(),
          method.getName());
      return joinPoint.proceed();
    }

    // Get current authenticated user ID
    String currentUserId;
    try {
      currentUserId = SecurityUtil.getCurrentUserId();
    } catch (Exception e) {
      log.error("Failed to get current user ID: {}", e.getMessage());
      throw new RuntimeException("Unauthorized access: User not authenticated", e);
    }

    // Validate that current user matches the userId parameter
    if (currentUserId == null || !currentUserId.equals(userIdFromParam)) {
      log.warn(
          "Unauthorized access attempt: currentUserId={}, paramUserId={}, method={}.{}",
          currentUserId,
          userIdFromParam,
          method.getDeclaringClass().getSimpleName(),
          method.getName());
      throw new RuntimeException("Unauthorized access");
    }

    log.debug(
        "User access validated: userId={}, method={}.{}",
        currentUserId,
        method.getDeclaringClass().getSimpleName(),
        method.getName());

    // Proceed with method execution
    return joinPoint.proceed();
  }
}
