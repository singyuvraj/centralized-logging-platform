package com.suljhaoo.backend.model.request.store;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStoreRequest {
  @Size(min = 1, max = 255, message = "Store name must be between 1 and 255 characters")
  private String storeName;

  private String storeAddress;

  private Boolean isActive;

  private Boolean isDeleted;
}
