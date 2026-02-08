package com.suljhaoo.backend.controller.auth;

import com.suljhaoo.backend.model.request.auth.CheckOtpRequest;
import com.suljhaoo.backend.model.request.auth.CheckPhoneRequest;
import com.suljhaoo.backend.model.request.auth.SendOtpRequest;
import com.suljhaoo.backend.model.request.auth.VerifyOtpRequest;
import com.suljhaoo.backend.model.response.auth.CheckPhoneResponse;
import com.suljhaoo.backend.model.response.auth.OtpCheckResult;
import com.suljhaoo.backend.model.response.auth.SendOtpResponse;
import com.suljhaoo.backend.model.response.auth.SignupData;
import com.suljhaoo.backend.model.response.auth.UserResponse;
import com.suljhaoo.backend.model.response.auth.VerifyOtpResponse;
import com.suljhaoo.backend.service.auth.OtpService;
import com.suljhaoo.backend.service.auth.SignupService;
import com.suljhaoo.backend.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/signup")
@RequiredArgsConstructor
public class SignupController {

  private final SignupService signupService;
  private final OtpService otpService;
  private final JwtUtil jwtUtil;

  // private final StoreService storeService; // Removed - no longer needed

  @Value("${spring.profiles.active:production}")
  private String activeProfile;

  @PostMapping("/check-phone")
  public ResponseEntity<CheckPhoneResponse> checkPhone(
      @Valid @RequestBody CheckPhoneRequest request) {
    String phoneNumber = request.getPhoneNumber().trim();

    // Validate phone number format
    if (!phoneNumber.matches("^[0-9]{10}$")) {
      CheckPhoneResponse response =
          CheckPhoneResponse.builder()
              .status("success")
              .exists(false)
              .message("Invalid phone number format")
              .build();
      return ResponseEntity.ok(response);
    }

    // Check if user already exists
    boolean userExists = signupService.checkPhoneNumberExists(phoneNumber);

    CheckPhoneResponse response =
        CheckPhoneResponse.builder()
            .status("success")
            .exists(userExists)
            .message(
                userExists
                    ? "This phone number is already registered. Please login instead."
                    : "Phone number is available")
            .build();

    return ResponseEntity.ok(response);
  }

  @PostMapping("/send-otp")
  public ResponseEntity<SendOtpResponse> sendOTP(@Valid @RequestBody SendOtpRequest request) {
    String phoneNumber = request.getPhoneNumber().trim();
    String name = request.getName().trim();

    // Validate phone number format
    if (!phoneNumber.matches("^[0-9]{10}$")) {
      throw new RuntimeException(
          "Invalid phone number format. Please provide a 10-digit phone number");
    }

    // Check if user already exists
    if (signupService.checkPhoneNumberExists(phoneNumber)) {
      throw new RuntimeException("User with this phone number already exists");
    }

    // Generate and store OTP (this also sends SMS)
    String otp = otpService.storeOTP(phoneNumber, name);

    SendOtpResponse.SendOtpResponseBuilder responseBuilder =
        SendOtpResponse.builder().status("success").message("OTP sent successfully");

    return ResponseEntity.ok(responseBuilder.build());
  }

  @PostMapping("/check-otp")
  public ResponseEntity<SendOtpResponse> checkOTP(@Valid @RequestBody CheckOtpRequest request) {
    String phoneNumber = request.getPhoneNumber().trim();
    String otp = request.getOtp().trim();

    // Check OTP (without deleting it)
    OtpCheckResult otpResult = otpService.checkOTP(phoneNumber, otp);
    if (!otpResult.isValid()) {
      throw new RuntimeException("Invalid or expired OTP");
    }

    SendOtpResponse response =
        SendOtpResponse.builder().status("success").message("OTP verified successfully").build();

    return ResponseEntity.ok(response);
  }

  @PostMapping("/verify-otp")
  public ResponseEntity<VerifyOtpResponse> verifyOTP(@Valid @RequestBody VerifyOtpRequest request) {
    String phoneNumber = request.getPhoneNumber().trim();
    String otp = request.getOtp().trim();
    String password = request.getPassword();

    // Validate password (minimum 6 characters)
    if (password.length() < 6) {
      throw new RuntimeException("Password must be at least 6 characters long");
    }

    // Verify OTP
    OtpCheckResult otpResult = otpService.verifyOTPForSignup(phoneNumber, otp);
    if (!otpResult.isValid()) {
      throw new RuntimeException("Invalid or expired OTP");
    }

    // Create user
    com.suljhaoo.backend.enity.auth.User user =
        signupService.createUser(otpResult.getName(), phoneNumber, password);

    // Generate JWT token
    String token =
        jwtUtil.generateToken(user.getId(), user.getPhoneNumber(), user.getRole().name());

    // Map user to response (without password)
    UserResponse userResponse =
        UserResponse.builder()
            .id(user.getId())
            .name(user.getName())
            .phoneNumber(user.getPhoneNumber())
            .email(user.getEmail())
            .role(user.getRole().name())
            .isActive(user.getIsActive())
            .lastLogin(user.getLastLogin())
            .createdAt(user.getCreatedAt())
            .build();

    // Build signup data with user and token
    SignupData signupData = SignupData.builder().user(userResponse).token(token).build();

    VerifyOtpResponse response =
        VerifyOtpResponse.builder()
            .status("success")
            .message("User created successfully")
            .data(signupData)
            .build();

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
