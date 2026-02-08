package com.suljhaoo.backend.model.request.expense;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateExpenseRequest {
  private String category;

  @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
  private BigDecimal amount;

  private String description;

  private String expenseDate; // Accepts date string (YYYY-MM-DD) or ISO datetime string

  @Pattern(
      regexp = "^(cash|upi|card|bank)$",
      flags = Pattern.Flag.CASE_INSENSITIVE,
      message = "Payment method must be one of: cash, upi, card, bank")
  private String paymentMethod;

  @Pattern(
      regexp = "^(Store|Staff|Bank|Govt Fees|Mobility)$",
      message = "Tag must be one of: Store, Staff, Bank, Govt Fees, Mobility")
  private String tag;
}
