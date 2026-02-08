package com.suljhaoo.backend.service.auth.impl;

import com.suljhaoo.backend.enity.auth.Otp;
import com.suljhaoo.backend.model.response.auth.OtpCheckResult;
import com.suljhaoo.backend.repository.auth.OtpRepository;
import com.suljhaoo.backend.service.auth.OtpService;
import com.suljhaoo.backend.util.Fast2SmsUtil;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

  private final OtpRepository otpRepository;
  private final Fast2SmsUtil fast2SmsUtil;

  @Value("${otp.expiry.minutes:1}")
  private int otpExpiryMinutes;

  private static final Random random = new Random();

  @Override
  @Transactional
  public String storeOTP(String phoneNumber, String name) {
    // Validate phone number format (should start with 6-9)
    if (phoneNumber == null || !phoneNumber.matches("^[6-9]\\d{9}$")) {
      throw new RuntimeException(
          "Invalid phone number. Please provide a valid 10-digit phone number starting with 6-9");
    }

    // Generate 6-digit OTP
    String otp = generateOTP();

    // Delete any existing OTP for this phone number
    otpRepository.deleteByPhoneNumber(phoneNumber.trim());

    // Store OTP with name
    Otp otpEntity =
        Otp.builder().phoneNumber(phoneNumber.trim()).otp(otp).name(name.trim()).build();

    otpRepository.save(otpEntity);

    log.info("OTP generated and stored for phone: {} (OTP: {})", phoneNumber, otp);

    // Send OTP asynchronously (non-blocking)
    sendOTPAsync(phoneNumber, otp, name);

    return otp;
  }

  @Override
  public OtpCheckResult checkOTP(String phoneNumber, String otp) {
    // Validate input
    if (phoneNumber == null || otp == null) {
      throw new RuntimeException("Phone number and OTP are required");
    }

    // Validate phone number format
    if (!phoneNumber.matches("^[6-9]\\d{9}$")) {
      throw new RuntimeException("Invalid phone number format");
    }

    // Find OTP document
    Optional<Otp> otpDoc = otpRepository.findByPhoneNumber(phoneNumber.trim());

    if (otpDoc.isEmpty()) {
      return new OtpCheckResult(false, null);
    }

    Otp otpEntity = otpDoc.get();

    // Check if OTP is expired (1 minute = 60 seconds)
    LocalDateTime expiryTime = otpEntity.getCreatedAt().plusMinutes(otpExpiryMinutes);
    if (LocalDateTime.now().isAfter(expiryTime)) {
      // Delete expired OTP
      otpRepository.delete(otpEntity);
      return new OtpCheckResult(false, null);
    }

    // Verify OTP
    if (!otpEntity.getOtp().equals(otp.trim())) {
      return new OtpCheckResult(false, null);
    }

    // OTP is valid, return name
    return new OtpCheckResult(true, otpEntity.getName());
  }

  @Override
  @Transactional
  public OtpCheckResult verifyOTPForSignup(String phoneNumber, String otp) {
    // Check OTP
    OtpCheckResult result = checkOTP(phoneNumber, otp);

    if (!result.isValid()) {
      return result;
    }

    // Delete OTP after successful verification
    otpRepository.deleteByPhoneNumber(phoneNumber.trim());

    return result;
  }

  @Override
  @Async
  public void sendOTPAsync(String phoneNumber, String otp, String name) {
    try {
      // Send OTP via Fast2SMS
      fast2SmsUtil.sendOTP(phoneNumber, otp);
      log.info("OTP sent successfully to phone: {}", phoneNumber);
    } catch (Exception e) {
      log.error(
          "Error sending OTP to phone: {}. OTP is already stored in database.", phoneNumber, e);
      // Don't throw exception - OTP is already stored, SMS failure shouldn't block the flow
      // The OTP can still be verified manually if needed
    }
  }

  private String generateOTP() {
    // Generate 6-digit OTP
    return String.format("%06d", random.nextInt(1000000));
  }
}
