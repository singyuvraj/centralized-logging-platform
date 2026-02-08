package com.suljhaoo.backend.security;

import com.suljhaoo.backend.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT Authentication Filter that validates JWT tokens and sets up Spring Security authentication.
 * Uses UserDetailsService to load user details and leverages Spring Security's built-in account
 * status validation (isEnabled, isAccountNonLocked, etc.).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;
  private final UserDetailsService userDetailsService;

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    // Skip JWT validation for OPTIONS requests (CORS preflight)
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      String origin = request.getHeader("Origin");
      log.debug("CORS Preflight Request - Origin: {}, Method: {}", origin, request.getMethod());
      filterChain.doFilter(request, response);
      return;
    }

    try {
      String jwt = getJwtFromRequest(request);

      if (StringUtils.hasText(jwt) && jwtUtil.validateToken(jwt)) {
        String userId = jwtUtil.extractId(jwt);
        String tokenRole = jwtUtil.extractRole(jwt);

        // Load user details from database using UserDetailsService
        // This will throw UsernameNotFoundException if user doesn't exist
        UserDetails userDetails = userDetailsService.loadUserByUsername(userId);

        // Verify role matches between token and database
        SecurityUser securityUser = (SecurityUser) userDetails;
        if (!securityUser.getRole().equalsIgnoreCase(tokenRole)) {
          log.warn(
              "Role mismatch for user {}: Token role={}, Database role={}",
              userId,
              tokenRole,
              securityUser.getRole());
          // Don't set authentication if role doesn't match
        } else if (!securityUser.isEnabled()) {
          log.debug("User account is deactivated: {}", userId);
          // Don't set authentication if account is disabled
        } else if (!securityUser.isAccountNonLocked()) {
          log.debug("User account is locked: {}", userId);
          // Don't set authentication if account is locked
        } else {
          // All validations passed - create authentication token with user details
          // Spring Security will use the UserDetails for authorization checks
          UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(
                  userDetails, null, userDetails.getAuthorities());

          authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authentication);
        }
      }
    } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
      log.debug("User not found during JWT validation: {}", e.getMessage());
      // Don't set authentication - user doesn't exist
    } catch (Exception e) {
      log.error("Cannot set user authentication: {}", e.getMessage());
      // Don't set authentication on error
    }

    filterChain.doFilter(request, response);
  }

  private String getJwtFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
      return bearerToken.substring(BEARER_PREFIX.length());
    }
    return null;
  }
}
