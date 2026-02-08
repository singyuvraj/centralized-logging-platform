package com.suljhaoo.backend.model.response.store;

import com.suljhaoo.backend.model.response.auth.StoreResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreSingleResponse {
  private String status;
  private String message;
  private StoreSingleData data;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class StoreSingleData {
    private StoreResponse store;
  }
}
