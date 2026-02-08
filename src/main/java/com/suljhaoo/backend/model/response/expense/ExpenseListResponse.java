package com.suljhaoo.backend.model.response.expense;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseListResponse {
  private String status;
  private String message;
  private ExpenseListData data;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ExpenseListData {
    private List<ExpenseResponse> expenses;
    private Long total;
    private Integer limit;
    private Integer skip;
  }
}
