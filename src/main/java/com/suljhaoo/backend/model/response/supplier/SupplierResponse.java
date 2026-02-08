package com.suljhaoo.backend.model.response.supplier;

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
public class SupplierResponse {
  private UUID id;
  private String userId;
  private String storeId;
  private String supplierType; // "mahajan" or "distributor" - stored directly in database
  private String name;
  private String nickName;
  private String phone;
  private String gstin;
  private String address;
  private String email;
  private String brands;
  private String salesman;
  private String salesPersonPhone;
  private String distributorName;
  private Integer maxNoOfCreditBills;
  private String maxCreditPeriod;
  // Legacy fields for backward compatibility with Node.js backend
  private String nickname; // For Mahajan type
  private String brandName; // For Distributor type (mapped from distributorName)
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
