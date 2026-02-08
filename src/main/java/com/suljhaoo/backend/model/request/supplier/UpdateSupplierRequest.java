package com.suljhaoo.backend.model.request.supplier;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSupplierRequest {
  @Pattern(
      regexp = "^(mahajan|distributor)$",
      flags = Pattern.Flag.CASE_INSENSITIVE,
      message = "Supplier type must be either 'mahajan' or 'distributor'")
  private String supplierType;

  private String name;

  // For Mahajan
  private String nickname;

  // For Company
  private String brandName;
  private String salesPersonName;
  private String salesPersonPhone;

  // Common fields
  private String phone;
  private String gstin;
  private String address;
  private String email;

  // Legacy fields (for backward compatibility)
  private String brands;
  private String salesman;

  // Credit management fields
  private Integer maxNoOfCreditBills;
  private String maxCreditPeriod; // Credit period (default: "EOM" - End Of Month)
}
