package com.suljhaoo.backend.service.auth.impl;

import com.suljhaoo.backend.enity.auth.User;
import com.suljhaoo.backend.model.request.auth.UpdateProfileRequest;
import com.suljhaoo.backend.model.response.auth.ProfileResponse;
import com.suljhaoo.backend.repository.auth.UserRepository;
import com.suljhaoo.backend.service.auth.ProfileService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

  private final UserRepository userRepository;

  @Override
  public ProfileResponse getProfile(String userId) {
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

    return mapToResponse(user);
  }

  @Override
  @Transactional
  public ProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

    // Validate that at least one field is provided
    boolean hasName = request.getName() != null && !request.getName().trim().isEmpty();
    boolean hasEmail = request.getEmail() != null;
    boolean hasAddress = request.getAddress() != null;
    boolean hasPhoneNumber =
        request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty();

    if (!hasName && !hasEmail && !hasAddress && !hasPhoneNumber) {
      throw new RuntimeException("At least one field must be provided");
    }

    // Update name if provided
    if (hasName) {
      String trimmedName = request.getName().trim();
      if (trimmedName.isEmpty()) {
        throw new RuntimeException("Name cannot be empty");
      }
      user.setName(trimmedName);
    }

    // Update email if provided
    if (hasEmail) {
      String trimmedEmail = request.getEmail().trim();
      // Allow empty string to clear email
      user.setEmail(trimmedEmail.isEmpty() ? null : trimmedEmail.toLowerCase());
    }

    // Update address if provided
    if (hasAddress) {
      String trimmedAddress = request.getAddress().trim();
      // Allow empty string to clear address
      user.setAddress(trimmedAddress.isEmpty() ? null : trimmedAddress);
    }

    // Update phone number if provided
    if (hasPhoneNumber) {
      String trimmedPhoneNumber = request.getPhoneNumber().trim();
      if (trimmedPhoneNumber.isEmpty()) {
        throw new RuntimeException("Phone number cannot be empty");
      }

      // Check if phone number is already taken by another user
      userRepository
          .findByPhoneNumber(trimmedPhoneNumber)
          .ifPresent(
              existingUser -> {
                if (!existingUser.getId().equals(userId)) {
                  throw new RuntimeException("Phone number is already registered to another user");
                }
              });

      user.setPhoneNumber(trimmedPhoneNumber);
    }

    user = userRepository.save(user);
    log.info("Profile updated for user: {}", userId);

    return mapToResponse(user);
  }

  private ProfileResponse mapToResponse(User user) {
    return ProfileResponse.builder()
        .id(user.getId())
        .name(user.getName())
        .phoneNumber(user.getPhoneNumber())
        .email(user.getEmail())
        .address(user.getAddress())
        .role(user.getRole().name())
        .isActive(user.getIsActive())
        .lastLogin(user.getLastLogin())
        .createdAt(user.getCreatedAt())
        .updatedAt(user.getUpdatedAt())
        .build();
  }
}
