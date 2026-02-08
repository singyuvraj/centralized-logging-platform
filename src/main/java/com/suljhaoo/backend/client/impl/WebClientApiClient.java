package com.suljhaoo.backend.client.impl;

import com.suljhaoo.backend.client.ExternalApiClient;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "external.api.client.type", havingValue = "web-client")
public class WebClientApiClient implements ExternalApiClient {

  private final WebClient webClient;

  @Override
  public <T, R> R post(String url, T request, Map<String, String> headers, Class<R> responseClass) {
    try {
      WebClient.RequestBodySpec requestSpec =
          webClient.post().uri(url).contentType(MediaType.APPLICATION_JSON);

      // Add headers if provided
      if (headers != null) {
        headers.forEach(requestSpec::header);
      }

      // Send request and get response
      R response =
          requestSpec
              .bodyValue(request)
              .retrieve()
              .bodyToMono(responseClass)
              .block(); // Blocking call - can be made reactive if needed

      log.debug("WebClient API call successful to: {}", url);
      return response;

    } catch (Exception e) {
      log.error("Error in WebClient API call to: {}", url, e);
      throw new RuntimeException("API call failed: " + e.getMessage(), e);
    }
  }
}
