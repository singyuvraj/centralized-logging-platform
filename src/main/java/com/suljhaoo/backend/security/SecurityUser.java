package com.suljhaoo.backend.security;

import com.suljhaoo.backend.enity.auth.User;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security UserDetails implementation that wraps the User entity. Provides account status
 * information to Spring Security for authentication checks.
 */
@Getter
public class SecurityUser implements UserDetails {

  private final String userId;
  private final String phoneNumber;
  private final String role;
  private final User user;
  private final Collection<? extends GrantedAuthority> authorities;

  /**
   * Creates a SecurityUser from a User entity. Extracts all necessary information and sets up
   * authorities based on user role.
   *
   * @param user The User entity from the database
   */
  public SecurityUser(User user) {
    this.user = user;
    this.userId = user.getId();
    this.phoneNumber = user.getPhoneNumber();
    this.role = user.getRole().name();
    this.authorities =
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return null; // JWT-based authentication doesn't use passwords in the filter
  }

  @Override
  public String getUsername() {
    return userId; // Use user ID as username in Spring Security context
  }

  /**
   * Checks if the account has not expired. Currently always returns true as we don't have account
   * expiration.
   *
   * @return true if account is not expired
   */
  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  /**
   * Checks if the account is not locked. Validates against the database state: checks accountLocked
   * flag and lockUntil timestamp.
   *
   * @return true if account is not locked
   */
  @Override
  public boolean isAccountNonLocked() {
    if (!Boolean.TRUE.equals(user.getAccountLocked())) {
      return true;
    }

    // If account is locked, check if lock period has expired
    if (user.getLockUntil() != null) {
      return user.getLockUntil().isBefore(LocalDateTime.now());
    }

    // Account is locked but no lockUntil timestamp (shouldn't happen, but be safe)
    return false;
  }

  /**
   * Checks if credentials have not expired. Always returns true for JWT-based authentication.
   *
   * @return true if credentials are not expired
   */
  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  /**
   * Checks if the account is enabled (active). Validates against the database isActive flag.
   *
   * @return true if account is active
   */
  @Override
  public boolean isEnabled() {
    return Boolean.TRUE.equals(user.getIsActive());
  }
}
