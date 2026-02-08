package com.suljhaoo.backend.model.response.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Fast2SmsResponse {
  @JsonProperty("return")
  private Boolean returnValue;

  @JsonProperty("request_id")
  private String requestId;

  @JsonProperty("message")
  private List<String> message;
}
