package com.suljhaoo.backend.model.response.expense;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {
  private UUID id;
  private String userId;
  private String storeId;
  private String category;
  private BigDecimal amount;
  private String description;
  private LocalDateTime expenseDate;
  private String paymentMethod;
  private String tag;
  private String billImageUrl;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
