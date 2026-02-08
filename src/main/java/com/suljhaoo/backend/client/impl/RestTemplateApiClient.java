package com.suljhaoo.backend.client.impl;

import com.suljhaoo.backend.client.ExternalApiClient;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "external.api.client.type",
    havingValue = "rest-template",
    matchIfMissing = true // Default to RestTemplate if property is not set
    )
public class RestTemplateApiClient implements ExternalApiClient {

  private final RestTemplate restTemplate;

  @Override
  public <T, R> R post(String url, T request, Map<String, String> headers, Class<R> responseClass) {
    try {
      // Prepare HTTP headers
      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.setContentType(MediaType.APPLICATION_JSON);

      if (headers != null) {
        headers.forEach(httpHeaders::set);
      }

      HttpEntity<T> entity = new HttpEntity<>(request, httpHeaders);

      // Send POST request
      ResponseEntity<R> response =
          restTemplate.exchange(url, HttpMethod.POST, entity, responseClass);

      log.debug(
          "RestTemplate API call successful to: {}. Response type: {}",
          url,
          responseClass.getName());

      R responseBody = response.getBody();
      if (responseBody == null) {
        log.warn("Response body is null for URL: {}", url);
      }

      return responseBody;

    } catch (org.springframework.web.client.HttpClientErrorException
        | org.springframework.web.client.HttpServerErrorException e) {
      log.error(
          "HTTP error in RestTemplate API call to: {}. Status: {}, Response body: {}",
          url,
          e.getStatusCode(),
          e.getResponseBodyAsString(),
          e);
      throw new RuntimeException("API call failed with HTTP error: " + e.getMessage(), e);
    } catch (org.springframework.http.converter.HttpMessageNotReadableException e) {
      log.error(
          "JSON deserialization error in RestTemplate API call to: {}. "
              + "This usually means the response structure doesn't match the expected class: {}",
          url,
          responseClass.getName(),
          e);
      throw new RuntimeException(
          "Failed to deserialize response. Expected type: "
              + responseClass.getName()
              + ". Error: "
              + e.getMessage(),
          e);
    } catch (Exception e) {
      log.error("Error in RestTemplate API call to: {}", url, e);
      throw new RuntimeException("API call failed: " + e.getMessage(), e);
    }
  }
}
