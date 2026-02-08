package com.suljhaoo.backend.model.request.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {
  @NotBlank(message = "Status is required")
  @Pattern(
      regexp = "^(ordered|received|cancelled|pending|closed)$",
      flags = Pattern.Flag.CASE_INSENSITIVE,
      message = "Status must be one of: ordered, received, cancelled, pending, closed")
  private String status;
}
