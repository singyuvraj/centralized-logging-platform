package com.suljhaoo.backend.model.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpResponse {
  private String status;
  private String message;
  private SignupData data;
}
