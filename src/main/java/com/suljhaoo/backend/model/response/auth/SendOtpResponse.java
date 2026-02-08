package com.suljhaoo.backend.model.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpResponse {
  private String status;
  private String message;
  private String otp; // Only in development mode
}
