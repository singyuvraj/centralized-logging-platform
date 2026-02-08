package com.suljhaoo.backend.model.response.sales;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleSingleResponse {
  private String status;
  private String message;
  private SaleSingleData data;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SaleSingleData {
    private SaleResponse sale;
  }
}
