package com.suljhaoo.backend.model.request.sales;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSaleRequest {
  @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
  private BigDecimal amount;

  @Pattern(
      regexp = "^(cash|upi|card|credit)$",
      flags = Pattern.Flag.CASE_INSENSITIVE,
      message = "Payment method must be one of: cash, upi, card, credit")
  private String paymentMethod;

  private String customerName;

  private String note;

  private LocalDateTime saleDate; // Optional: date when sale occurred (can be in the past)
}
