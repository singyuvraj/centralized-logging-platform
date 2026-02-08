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
public class SalesListResult {
  private List<SaleResponse> sales;
  private Long total;
}
