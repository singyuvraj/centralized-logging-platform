package com.suljhaoo.backend.service.auth;

import com.suljhaoo.backend.model.request.auth.UpdateProfileRequest;
import com.suljhaoo.backend.model.response.auth.ProfileResponse;

public interface ProfileService {
  ProfileResponse getProfile(String userId);

  ProfileResponse updateProfile(String userId, UpdateProfileRequest request);
}
