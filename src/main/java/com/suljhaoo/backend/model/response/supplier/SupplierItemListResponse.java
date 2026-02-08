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
public class SupplierItemListResponse {
  private String status;
  private String message;
  private SupplierItemListData data;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SupplierItemListData {
    private List<SupplierItemResponse> items;
    private Long total;
    private Integer limit;
    private Integer skip;
  }
}
