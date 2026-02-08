package com.suljhaoo.backend.model.request.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class CreateOrderRequest {
  @NotBlank(message = "Supplier ID is required")
  private String supplierId;

  @NotBlank(message = "Supplier name is required")
  private String supplierName;

  @NotBlank(message = "Supplier phone is required")
  private String supplierPhone;

  @NotEmpty(message = "Order items are required")
  @Valid
  private List<OrderItemRequest> items;

  @NotNull(message = "Total items count is required")
  @Positive(message = "Total items must be at least 1")
  private Integer totalItems;

  private String orderDate; // Optional, accepts ISO datetime string

  @Pattern(
      regexp = "^(ordered|received|cancelled|pending|closed)$",
      flags = Pattern.Flag.CASE_INSENSITIVE,
      message = "Status must be one of: ordered, received, cancelled, pending, closed")
  private String status; // Optional, defaults to 'ordered'
}
