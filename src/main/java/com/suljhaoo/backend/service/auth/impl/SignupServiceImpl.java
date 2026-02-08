package com.suljhaoo.backend.service.auth.impl;

import com.suljhaoo.backend.enity.auth.User;
import com.suljhaoo.backend.enity.auth.UserRole;
import com.suljhaoo.backend.repository.auth.UserRepository;
import com.suljhaoo.backend.service.auth.SignupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignupServiceImpl implements SignupService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public boolean checkPhoneNumberExists(String phoneNumber) {
    return userRepository.findByPhoneNumber(phoneNumber).isPresent();
  }

  @Override
  @Transactional
  public User createUser(String name, String phoneNumber, String password) {
    // Check if user already exists
    if (checkPhoneNumberExists(phoneNumber)) {
      throw new RuntimeException("User with this phone number already exists");
    }

    // Hash password
    String hashedPassword = passwordEncoder.encode(password);

    // Create user with default values
    User user =
        User.builder()
            .name(name.trim())
            .phoneNumber(phoneNumber.trim())
            .password(hashedPassword)
            .email("") // Empty email as per requirement
            .role(UserRole.shopowner)
            .isActive(true)
            .lastLogin(null) // Will be set on first login
            .loginAttempts(0)
            .accountLocked(false)
            .build();

    return userRepository.save(user);
  }
}
