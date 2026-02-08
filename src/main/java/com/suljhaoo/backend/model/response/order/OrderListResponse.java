package com.suljhaoo.backend.model.response.order;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderListResponse {
  private String status;
  private String message;
  private OrderListData data;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OrderListData {
    private List<OrderResponse> orders;
    private Long total;
    private Integer limit;
    private Integer skip;
  }
}
