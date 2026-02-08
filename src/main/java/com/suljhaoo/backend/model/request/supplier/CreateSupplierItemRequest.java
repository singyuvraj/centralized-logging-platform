package com.suljhaoo.backend.model.request.supplier;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSupplierItemRequest {
  @NotBlank(message = "Item name is required")
  private String name;

  @NotBlank(message = "Unit is required")
  private String unit;

  @NotBlank(message = "Category is required")
  private String category;

  private String stockItemId; // Optional reference to stock item

  private BigDecimal quantity; // Optional initial quantity for stock item

  private BigDecimal minLevel; // Optional minimum level for stock item

  private String description; // Optional description for stock item
}
