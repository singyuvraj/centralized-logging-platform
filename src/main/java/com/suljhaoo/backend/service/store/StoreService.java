package com.suljhaoo.backend.service.store;

import com.suljhaoo.backend.model.request.store.CreateStoreRequest;
import com.suljhaoo.backend.model.request.store.UpdateStoreRequest;
import com.suljhaoo.backend.model.response.auth.StoreResponse;
import java.util.List;

public interface StoreService {
  StoreResponse createStore(String userId, CreateStoreRequest request);

  List<StoreResponse> getAllStores(String userId);

  StoreResponse getStoreById(String storeId, String userId);

  StoreResponse updateStore(String storeId, String userId, UpdateStoreRequest request);

  void deleteStore(String storeId, String userId);
}
