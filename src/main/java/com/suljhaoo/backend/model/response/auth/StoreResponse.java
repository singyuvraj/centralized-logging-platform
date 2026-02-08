package com.suljhaoo.backend.model.response.auth;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreResponse {
  private String id;
  private String userId;
  private String storeName;
  private String storeAddress;
  private Boolean isActive;
  private Boolean isDeleted;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
