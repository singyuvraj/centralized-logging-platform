package com.suljhaoo.backend.model.response.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadResponse {
  private String status;
  private String message;
  private BulkUploadData data;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BulkUploadData {
    private Integer created;
    private Integer updated;
    private Integer total;
    private java.util.List<String> errors;
    private Integer totalErrors;
  }
}
