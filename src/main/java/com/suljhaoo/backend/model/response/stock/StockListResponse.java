package com.suljhaoo.backend.model.response.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockListResponse {
  private String status;
  private StockListData data;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class StockListData {
    private java.util.List<StockResponse> stocks;
    private Long total;
    private Integer limit;
    private Integer skip;
  }
}
