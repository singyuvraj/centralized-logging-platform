package com.suljhaoo.backend.model.response.sales;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesListResponse {
  private String status;
  private String message;
  private SalesListData data;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SalesListData {
    private List<SaleResponse> sales;
    private Long total;
    private Integer limit;
    private Integer skip;
  }
}
