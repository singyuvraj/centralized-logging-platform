package com.suljhaoo.backend.model.request.stock;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStockRequest {
  private String name;

  @DecimalMin(value = "0.0", message = "Quantity cannot be negative")
  private BigDecimal quantity;

  @DecimalMin(value = "0.0", message = "Minimum level cannot be negative")
  private BigDecimal minLevel;

  private String unit;

  private String category;

  @DecimalMin(value = "0.0", message = "Unit price cannot be negative")
  private BigDecimal unitPrice;

  private String description;

  private UUID supplierId;

  private String supplierName;
}
