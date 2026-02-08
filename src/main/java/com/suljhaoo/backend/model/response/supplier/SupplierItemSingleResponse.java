package com.suljhaoo.backend.model.response.supplier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierItemSingleResponse {
  private String status;
  private String message;
  private SupplierItemSingleData data;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SupplierItemSingleData {
    private SupplierItemResponse item;
  }
}
