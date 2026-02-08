package com.suljhaoo.backend.service.auth.impl;

import com.suljhaoo.backend.enity.auth.User;
import com.suljhaoo.backend.enity.auth.UserRole;
import com.suljhaoo.backend.model.request.auth.LoginRequest;
import com.suljhaoo.backend.model.response.auth.LoginData;
import com.suljhaoo.backend.model.response.auth.UserResponse;
import com.suljhaoo.backend.repository.auth.UserRepository;
import com.suljhaoo.backend.service.auth.AuthService;
import com.suljhaoo.backend.util.JwtUtil;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final UserRepository userRepository;
  // private final StoreService storeService; // Removed - no longer needed
  private final JwtUtil jwtUtil;
  private final PasswordEncoder passwordEncoder;

  private static final int MAX_LOGIN_ATTEMPTS = 5;
  private static final long LOCK_TIME_MINUTES = 30;

  @Override
  @Transactional
  public LoginData login(LoginRequest request) {
    String phoneNumber = request.getPhoneNumber().trim();

    // Find user by phone number
    User user =
        userRepository
            .findByPhoneNumber(phoneNumber)
            .orElseThrow(() -> new RuntimeException("Invalid phone number or password"));

    // Check if account is locked
    if (user.getAccountLocked() && user.getLockUntil() != null) {
      if (user.getLockUntil().isAfter(LocalDateTime.now())) {
        long lockTimeRemaining =
            java.time.Duration.between(LocalDateTime.now(), user.getLockUntil()).toMinutes();
        throw new RuntimeException(
            "Account is locked. Please try again after " + lockTimeRemaining + " minutes.");
      } else {
        // Lock period has expired, unlock the account
        user.setAccountLocked(false);
        user.setLockUntil(null);
        user.setLoginAttempts(0);
      }
    }

    // Check if account is active
    if (!user.getIsActive()) {
      throw new RuntimeException("Account is deactivated. Please contact support.");
    }

    // Check if user has valid role (admin or shopowner)
    if (user.getRole() != UserRole.admin && user.getRole() != UserRole.shopowner) {
      throw new RuntimeException("Invalid user role. Access denied.");
    }

    // Verify password - password is always fetched since it's not marked with select=false
    boolean isPasswordValid = passwordEncoder.matches(request.getPassword(), user.getPassword());

    if (!isPasswordValid) {
      // Increment login attempts
      int loginAttempts = (user.getLoginAttempts() != null ? user.getLoginAttempts() : 0) + 1;

      if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
        // Lock account for 30 minutes
        LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(LOCK_TIME_MINUTES);
        user.setLoginAttempts(loginAttempts);
        user.setAccountLocked(true);
        user.setLockUntil(lockUntil);
        userRepository.save(user);
        throw new RuntimeException(
            "Too many failed login attempts. Account locked for 30 minutes.");
      } else {
        // Update login attempts
        user.setLoginAttempts(loginAttempts);
        userRepository.save(user);
        throw new RuntimeException(
            "Invalid phone number or password. "
                + (MAX_LOGIN_ATTEMPTS - loginAttempts)
                + " attempts remaining.");
      }
    }

    // Reset login attempts and unlock account on successful login
    user.setLoginAttempts(0);
    user.setAccountLocked(false);
    user.setLockUntil(null);
    user.setLastLogin(LocalDateTime.now());
    userRepository.save(user);

    // Generate token
    String token =
        jwtUtil.generateToken(user.getId(), user.getPhoneNumber(), user.getRole().name());

    // Map user to response (without password)
    UserResponse userResponse =
        UserResponse.builder()
            .id(user.getId())
            .name(user.getName())
            .phoneNumber(user.getPhoneNumber())
            .email(user.getEmail())
            .role(user.getRole().name())
            .isActive(user.getIsActive())
            .lastLogin(user.getLastLogin())
            .createdAt(user.getCreatedAt())
            .build();

    return LoginData.builder().user(userResponse).token(token).build();
  }
}
