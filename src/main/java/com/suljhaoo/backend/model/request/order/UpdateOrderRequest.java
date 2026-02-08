package com.suljhaoo.backend.model.request.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderRequest {
  @Valid private List<OrderItemRequest> items;

  @Positive(message = "Total items must be at least 1")
  private Integer totalItems;

  private String orderDate; // Optional, accepts ISO datetime string

  @Pattern(
      regexp = "^(ordered|received|cancelled|pending|closed)$",
      flags = Pattern.Flag.CASE_INSENSITIVE,
      message = "Status must be one of: ordered, received, cancelled, pending, closed")
  private String status;
}
