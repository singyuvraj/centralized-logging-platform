package com.suljhaoo.backend.model.response.store;

import com.suljhaoo.backend.model.response.auth.StoreResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreListResponse {
  private String status;
  private StoreListData data;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class StoreListData {
    private List<StoreResponse> stores;
  }
}
