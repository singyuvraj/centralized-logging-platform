package com.suljhaoo.backend.model.response.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Result of OTP verification check */
@Getter
@AllArgsConstructor
public class OtpCheckResult {
  private final boolean valid;
  private final String name;
}
