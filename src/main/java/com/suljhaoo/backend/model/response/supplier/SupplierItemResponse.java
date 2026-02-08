package com.suljhaoo.backend.model.response.supplier;

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
public class SupplierItemResponse {
  private UUID id;
  private String userId;
  private String supplierId;
  private String storeId;
  private String stockItemId;
  private String name;
  private String unit;
  private String category;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
