package com.suljhaoo.backend.security;

import com.suljhaoo.backend.enity.auth.User;
import com.suljhaoo.backend.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserDetailsService implementation that loads user details from the database. Used by Spring
 * Security for authentication and authorization.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;

  /**
   * Loads user by user ID (not username, since we use user ID as the identifier). This is called
   * during JWT authentication to validate user status.
   *
   * @param userId The user ID (used as username in Spring Security context)
   * @return UserDetails containing user information and authorities
   * @throws UsernameNotFoundException if user is not found
   */
  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> {
                  log.debug("User not found: {}", userId);
                  return new UsernameNotFoundException("User not found: " + userId);
                });

    return new SecurityUser(user);
  }
}
