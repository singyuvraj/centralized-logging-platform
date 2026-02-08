package com.suljhaoo.backend.model.response.supplier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierSingleResponse {
  private String status;
  private String message;
  private SupplierSingleData data;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SupplierSingleData {
    private SupplierResponse supplier;
  }
}
