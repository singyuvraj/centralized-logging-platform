package com.suljhaoo.backend.model.response.stock;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockListResult {
  private List<StockResponse> stocks;
  private Long total;
}
