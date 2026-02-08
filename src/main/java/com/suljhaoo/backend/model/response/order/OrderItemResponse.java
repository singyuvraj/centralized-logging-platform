package com.suljhaoo.backend.model.response.order;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
  private String itemId;
  private String itemName;
  private BigDecimal quantity;
  private String unit;
}
