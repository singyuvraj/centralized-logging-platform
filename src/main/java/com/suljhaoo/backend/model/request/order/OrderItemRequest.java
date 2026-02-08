package com.suljhaoo.backend.model.request.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {
  @NotBlank(message = "Item ID is required")
  private String itemId;

  @NotBlank(message = "Item name is required")
  private String itemName;

  @NotNull(message = "Quantity is required")
  @Positive(message = "Quantity must be greater than 0")
  private BigDecimal quantity;

  @NotBlank(message = "Unit is required")
  private String unit;
}
