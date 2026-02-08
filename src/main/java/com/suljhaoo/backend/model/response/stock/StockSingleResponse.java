package com.suljhaoo.backend.model.response.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockSingleResponse {
  private String status;
  private String message;
  private StockSingleData data;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class StockSingleData {
    private StockResponse stock;
  }
}
