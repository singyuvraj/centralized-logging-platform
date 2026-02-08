package com.suljhaoo.backend.util;

import com.suljhaoo.backend.security.SecurityUser;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

  /**
   * Get the current authenticated user ID from SecurityContext
   *
   * @return User ID as String, or null if not authenticated
   */
  public static String getCurrentUserId() {
    return Optional.ofNullable(SecurityContextHolder.getContext())
        .map(SecurityContext::getAuthentication)
        .filter(Authentication::isAuthenticated)
        .map(Authentication::getPrincipal)
        .filter(principal -> principal instanceof SecurityUser)
        .map(principal -> (SecurityUser) principal)
        .map(SecurityUser::getUserId)
        .orElseThrow();
  }
}
