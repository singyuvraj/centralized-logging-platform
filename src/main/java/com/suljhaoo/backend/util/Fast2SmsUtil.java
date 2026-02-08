package com.suljhaoo.backend.util;

import com.suljhaoo.backend.client.ExternalApiClient;
import com.suljhaoo.backend.model.request.auth.Fast2SmsRequest;
import com.suljhaoo.backend.model.response.auth.Fast2SmsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class Fast2SmsUtil {

  private final ExternalApiClient externalApiClient;

  @Value("${fast2sms.api.key:}")
  private String apiKey;

  @Value("${fast2sms.api.url:https://www.fast2sms.com/dev/bulkV2}")
  private String apiUrl;

  @Value("${fast2sms.sender.id:}")
  private String senderId;

  @Value("${fast2sms.message.id:}")
  private String messageId;

  /**
   * Send OTP via Fast2SMS API
   *
   * @param phoneNumber 10-digit phone number (without country code)
   * @param otp 6-digit OTP code
   * @throws RuntimeException if SMS sending fails
   */
  public void sendOTP(String phoneNumber, String otp) {
    // Validate configuration
    if (apiKey == null || apiKey.isEmpty()) {
      log.warn("FAST2SMS_API_KEY is not configured. Skipping SMS sending.");
      return;
    }

    if (senderId == null || senderId.isEmpty()) {
      log.warn("SENDER_ID is not configured. Skipping SMS sending.");
      return;
    }

    if (messageId == null || messageId.isEmpty()) {
      log.warn("MESSAGE_ID is not configured. Skipping SMS sending.");
      return;
    }

    try {
      // Prepare request body
      Fast2SmsRequest request =
          Fast2SmsRequest.builder()
              .route("dlt")
              .senderId(senderId)
              .message(messageId)
              .variablesValues(otp)
              .scheduleTime("")
              .flash(0)
              .numbers("91" + phoneNumber) // Add country code
              .build();

      // Send request using ExternalApiClient
      Fast2SmsResponse response = externalApiClient.postFast2Sms(apiUrl, request, apiKey);

      // Check response
      if (response != null && Boolean.TRUE.equals(response.getReturnValue())) {
        String successMessage =
            response.getMessage() != null && !response.getMessage().isEmpty()
                ? response.getMessage().get(0)
                : "SMS sent successfully";
        log.info(
            "OTP sent successfully to phone: {}. Request ID: {}. Message: {}",
            phoneNumber,
            response.getRequestId(),
            successMessage);
      } else {
        String errorMessage =
            response != null && response.getMessage() != null && !response.getMessage().isEmpty()
                ? response.getMessage().get(0)
                : "Unknown error";
        log.error("Failed to send OTP to phone: {}. Error: {}", phoneNumber, errorMessage);
        throw new RuntimeException("Failed to send SMS: " + errorMessage);
      }

    } catch (Exception e) {
      log.error("Error sending OTP to phone: {}", phoneNumber, e);
      throw new RuntimeException("Failed to send OTP: " + e.getMessage(), e);
    }
  }
}
