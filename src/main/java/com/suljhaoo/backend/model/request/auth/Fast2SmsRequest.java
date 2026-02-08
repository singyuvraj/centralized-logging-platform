package com.suljhaoo.backend.model.request.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Fast2SmsRequest {
  private String route;

  @JsonProperty("sender_id")
  private String senderId;

  private String message;

  @JsonProperty("variables_values")
  private String variablesValues;

  @JsonProperty("schedule_time")
  private String scheduleTime;

  private Integer flash;

  private String numbers;
}
