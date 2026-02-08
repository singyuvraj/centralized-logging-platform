package com.suljhaoo.backend.model.response.order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
  private UUID id;
  private String userId;
  private String storeId;
  private String supplierId;
  private String supplierName;
  private String supplierPhone;
  private List<OrderItemResponse> items;
  private Integer totalItems;
  private LocalDateTime orderDate;
  private String status;
  private Boolean addedToStock;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
