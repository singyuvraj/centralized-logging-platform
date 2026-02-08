package com.suljhaoo.backend.model.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyOtpRequest {
  @NotBlank(message = "Phone number is required")
  @Pattern(
      regexp = "^[0-9]{10}$",
      message = "Invalid phone number format. Please provide a 10-digit phone number")
  private String phoneNumber;

  @NotBlank(message = "OTP is required")
  @Pattern(regexp = "^\\d{6}$", message = "Invalid OTP format. OTP must be 6 digits")
  private String otp;

  @NotBlank(message = "Password is required")
  @Size(min = 6, message = "Password must be at least 6 characters long")
  private String password;
}
