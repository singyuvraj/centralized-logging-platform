package com.suljhaoo.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

  @Bean
  @ConditionalOnProperty(name = "external.api.client.type", havingValue = "web-client")
  public WebClient webClient() {
    return WebClient.builder()
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB
        .build();
  }
}
