package com.suljhaoo.backend.model.response.supplier;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierListResult {
  private List<SupplierResponse> suppliers;
  private Long total;
}
