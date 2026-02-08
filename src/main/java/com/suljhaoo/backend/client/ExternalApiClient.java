package com.suljhaoo.backend.client;

import com.suljhaoo.backend.model.request.auth.Fast2SmsRequest;
import com.suljhaoo.backend.model.response.auth.Fast2SmsResponse;

/**
 * Interface for external API clients Supports multiple implementations (RestTemplate, WebClient,
 * etc.)
 */
public interface ExternalApiClient {

  /**
   * Send HTTP POST request to external API
   *
   * @param url The API endpoint URL
   * @param request The request object
   * @param headers HTTP headers (can be null)
   * @param responseClass The response class type
   * @return The response object
   * @param <T> Request type
   * @param <R> Response type
   */
  <T, R> R post(
      String url, T request, java.util.Map<String, String> headers, Class<R> responseClass);

  /**
   * Send HTTP POST request to Fast2SMS API
   *
   * @param url The Fast2SMS API endpoint URL
   * @param request The Fast2SMS request
   * @param apiKey The API key for authorization
   * @return The Fast2SMS response
   */
  default Fast2SmsResponse postFast2Sms(String url, Fast2SmsRequest request, String apiKey) {
    java.util.Map<String, String> headers = new java.util.HashMap<>();
    headers.put("authorization", apiKey);
    headers.put("Content-Type", "application/json");

    return post(url, request, headers, Fast2SmsResponse.class);
  }
}
