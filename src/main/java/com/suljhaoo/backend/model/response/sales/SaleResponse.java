package com.suljhaoo.backend.model.response.sales;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleResponse {
  private String id;
  private String userId;
  private String storeId;
  private BigDecimal amount;
  private String paymentMethod;
  private String customerName;
  private String note;
  private LocalDateTime saleDate;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Integer version;
  private String updatedBy;
}
