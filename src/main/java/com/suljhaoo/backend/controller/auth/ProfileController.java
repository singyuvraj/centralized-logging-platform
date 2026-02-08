package com.suljhaoo.backend.controller.auth;

import com.suljhaoo.backend.model.request.auth.UpdateProfileRequest;
import com.suljhaoo.backend.model.response.auth.ProfileSingleResponse;
import com.suljhaoo.backend.service.auth.ProfileService;
import com.suljhaoo.backend.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

  private final ProfileService profileService;

  /** Get user profile GET /api/profile */
  @GetMapping
  public ResponseEntity<ProfileSingleResponse> getProfile() {
    String userId = SecurityUtil.getCurrentUserId();
    var profile = profileService.getProfile(userId);

    ProfileSingleResponse response =
        ProfileSingleResponse.builder()
            .status("success")
            .message("Profile retrieved successfully")
            .data(ProfileSingleResponse.ProfileSingleData.builder().profile(profile).build())
            .build();

    return ResponseEntity.ok(response);
  }

  /** Update user profile PUT /api/profile */
  @PutMapping
  public ResponseEntity<ProfileSingleResponse> updateProfile(
      @Valid @RequestBody UpdateProfileRequest request) {
    String userId = SecurityUtil.getCurrentUserId();
    var profile = profileService.updateProfile(userId, request);

    ProfileSingleResponse response =
        ProfileSingleResponse.builder()
            .status("success")
            .message("Profile updated successfully")
            .data(ProfileSingleResponse.ProfileSingleData.builder().profile(profile).build())
            .build();

    return ResponseEntity.ok(response);
  }
}
