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
public class ExpenseListResult {
  private List<ExpenseResponse> expenses;
  private Long total;
}
