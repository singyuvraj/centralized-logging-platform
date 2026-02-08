package com.suljhaoo.backend.model.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileSingleResponse {
  private String status;
  private String message;
  private ProfileSingleData data;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProfileSingleData {
    private ProfileResponse profile;
  }
}
