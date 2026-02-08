package com.suljhaoo.backend.model.request.supplier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSupplierItemRequest {
  private String name;
  private String unit;
  private String category;
}
