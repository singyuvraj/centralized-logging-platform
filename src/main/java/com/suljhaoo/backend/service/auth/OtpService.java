package com.suljhaoo.backend.service.auth;

import com.suljhaoo.backend.model.response.auth.OtpCheckResult;

public interface OtpService {
  String storeOTP(String phoneNumber, String name);

  OtpCheckResult checkOTP(String phoneNumber, String otp);

  OtpCheckResult verifyOTPForSignup(String phoneNumber, String otp);

  /**
   * Send OTP via SMS asynchronously
   *
   * @param phoneNumber Phone number to send OTP to
   * @param otp The OTP code to send
   * @param name Name associated with the phone number
   */
  void sendOTPAsync(String phoneNumber, String otp, String name);
}
