package com.suljhaoo.backend.model.response.expense;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSingleResponse {
  private String status;
  private String message;
  private ExpenseSingleData data;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ExpenseSingleData {
    private ExpenseResponse expense;
  }
}
