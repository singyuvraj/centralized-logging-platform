package com.suljhaoo.backend.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HealthResponse {
  private String status;

  @JsonProperty("app_name")
  private String appName;

  private String version;
  private String message;

  @JsonProperty("time_stamp")
  @JsonFormat(pattern = "YYYY-MM-dd hh:mm:ss")
  @Builder.Default
  private LocalDateTime timeStamp = LocalDateTime.now();
}
