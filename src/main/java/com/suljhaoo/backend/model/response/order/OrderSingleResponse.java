package com.suljhaoo.backend.model.response.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSingleResponse {
  private String status;
  private String message;
  private OrderSingleData data;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OrderSingleData {
    private OrderResponse order;
  }
}
