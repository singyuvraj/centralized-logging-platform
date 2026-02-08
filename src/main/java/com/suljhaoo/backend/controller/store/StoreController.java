package com.suljhaoo.backend.controller.store;

import com.suljhaoo.backend.model.request.store.CreateStoreRequest;
import com.suljhaoo.backend.model.request.store.UpdateStoreRequest;
import com.suljhaoo.backend.model.response.auth.StoreResponse;
import com.suljhaoo.backend.model.response.store.StoreListResponse;
import com.suljhaoo.backend.model.response.store.StoreSingleResponse;
import com.suljhaoo.backend.service.store.StoreService;
import com.suljhaoo.backend.util.SecurityUtil;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StoreController {

  private final StoreService storeService;

  /** Create a new store POST /api/stores */
  @PostMapping
  public ResponseEntity<StoreSingleResponse> createStore(
      @Valid @RequestBody CreateStoreRequest request) {
    String userId = SecurityUtil.getCurrentUserId();
    StoreResponse store = storeService.createStore(userId, request);

    StoreSingleResponse response =
        StoreSingleResponse.builder()
            .status("success")
            .message("Store created successfully")
            .data(StoreSingleResponse.StoreSingleData.builder().store(store).build())
            .build();

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /** Get all stores for the authenticated user GET /api/stores */
  @GetMapping
  public ResponseEntity<StoreListResponse> getAllStores() {
    String userId = SecurityUtil.getCurrentUserId();
    List<StoreResponse> stores = storeService.getAllStores(userId);

    StoreListResponse response =
        StoreListResponse.builder()
            .status("success")
            .data(StoreListResponse.StoreListData.builder().stores(stores).build())
            .build();

    return ResponseEntity.ok(response);
  }

  /** Get store by ID GET /api/stores/:id */
  @GetMapping("/{id}")
  public ResponseEntity<StoreSingleResponse> getStoreById(@PathVariable String id) {
    String userId = SecurityUtil.getCurrentUserId();
    StoreResponse store = storeService.getStoreById(id, userId);

    StoreSingleResponse response =
        StoreSingleResponse.builder()
            .status("success")
            .data(StoreSingleResponse.StoreSingleData.builder().store(store).build())
            .build();

    return ResponseEntity.ok(response);
  }

  /** Update a store PUT /api/stores/:id */
  @PutMapping("/{id}")
  public ResponseEntity<StoreSingleResponse> updateStore(
      @PathVariable String id, @Valid @RequestBody UpdateStoreRequest request) {
    String userId = SecurityUtil.getCurrentUserId();
    StoreResponse store = storeService.updateStore(id, userId, request);

    StoreSingleResponse response =
        StoreSingleResponse.builder()
            .status("success")
            .message("Store updated successfully")
            .data(StoreSingleResponse.StoreSingleData.builder().store(store).build())
            .build();

    return ResponseEntity.ok(response);
  }

  /** Delete a store DELETE /api/stores/:id */
  @DeleteMapping("/{id}")
  public ResponseEntity<StoreSingleResponse> deleteStore(@PathVariable String id) {
    String userId = SecurityUtil.getCurrentUserId();
    storeService.deleteStore(id, userId);

    StoreSingleResponse response =
        StoreSingleResponse.builder()
            .status("success")
            .message("Store deleted successfully")
            .build();

    return ResponseEntity.ok(response);
  }
}
