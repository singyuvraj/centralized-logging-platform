package com.suljhaoo.backend.model.response.stock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockResponse {
  private UUID id;
  private String storeId;
  private String name;
  private BigDecimal quantity;
  private BigDecimal minLevel;
  private String unit;
  private String category;
  private BigDecimal unitPrice;
  private String description;
  private UUID supplierId;
  private String supplierName;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
