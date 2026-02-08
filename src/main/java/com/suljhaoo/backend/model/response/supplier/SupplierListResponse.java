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
public class SupplierListResponse {
  private String status;
  private String message;
  private SupplierListData data;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SupplierListData {
    private List<SupplierResponse> suppliers;
    private Long total;
    private Integer limit;
    private Integer skip;
  }
}
