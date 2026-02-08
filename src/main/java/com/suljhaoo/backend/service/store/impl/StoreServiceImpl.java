package com.suljhaoo.backend.service.store.impl;

import com.suljhaoo.backend.enity.auth.Store;
import com.suljhaoo.backend.enity.auth.User;
import com.suljhaoo.backend.model.request.store.CreateStoreRequest;
import com.suljhaoo.backend.model.request.store.UpdateStoreRequest;
import com.suljhaoo.backend.model.response.auth.StoreResponse;
import com.suljhaoo.backend.repository.auth.StoreRepository;
import com.suljhaoo.backend.repository.auth.UserRepository;
import com.suljhaoo.backend.service.store.StoreService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreServiceImpl implements StoreService {

  private final StoreRepository storeRepository;
  private final UserRepository userRepository;

  @Override
  @Transactional
  public StoreResponse createStore(String userId, CreateStoreRequest request) {
    // Validate user exists
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

    // Create store - always set isActive to true and isDeleted to false for new stores
    Store store =
        Store.builder()
            .user(user)
            .storeName(request.getStoreName().trim())
            .storeAddress(
                request.getStoreAddress() != null ? request.getStoreAddress().trim() : null)
            .isActive(request.getIsActive() != null ? request.getIsActive() : true)
            .isDeleted(false)
            .build();

    store = storeRepository.save(store);

    return mapToResponse(store);
  }

  @Override
  public List<StoreResponse> getAllStores(String userId) {
    List<Store> stores = storeRepository.findByUser_IdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
    return stores.stream().map(this::mapToResponse).collect(Collectors.toList());
  }

  @Override
  public StoreResponse getStoreById(String storeId, String userId) {
    Store store =
        storeRepository
            .findByIdAndUser_IdAndIsDeletedFalse(storeId, userId)
            .orElseThrow(() -> new RuntimeException("Store not found"));
    return mapToResponse(store);
  }

  @Override
  @Transactional
  public StoreResponse updateStore(String storeId, String userId, UpdateStoreRequest request) {
    // Find store regardless of isDeleted status (to allow restoration)
    Store store =
        storeRepository
            .findByIdAndUser_Id(storeId, userId)
            .orElseThrow(() -> new RuntimeException("Store not found"));

    // Update fields
    if (request.getStoreName() != null) {
      store.setStoreName(request.getStoreName().trim());
    }
    if (request.getStoreAddress() != null) {
      store.setStoreAddress(request.getStoreAddress().trim());
    }
    if (request.getIsActive() != null) {
      store.setIsActive(request.getIsActive());
    }
    if (request.getIsDeleted() != null) {
      store.setIsDeleted(request.getIsDeleted());
    }

    store = storeRepository.save(store);
    return mapToResponse(store);
  }

  @Override
  @Transactional
  public void deleteStore(String storeId, String userId) {
    Store store =
        storeRepository
            .findByIdAndUser_IdAndIsDeletedFalse(storeId, userId)
            .orElseThrow(() -> new RuntimeException("Store not found"));

    // Check if this is the only store (excluding deleted stores)
    long storeCount = storeRepository.countByUser_IdAndIsDeletedFalse(userId);
    if (storeCount == 1) {
      throw new RuntimeException(
          "Cannot delete the only store. Please create another store first.");
    }

    // Soft delete: set isDeleted to true
    store.setIsDeleted(true);
    storeRepository.save(store);
  }

  private StoreResponse mapToResponse(Store store) {
    return StoreResponse.builder()
        .id(store.getId())
        .userId(store.getUserId())
        .storeName(store.getStoreName())
        .storeAddress(store.getStoreAddress())
        .isActive(store.getIsActive())
        .isDeleted(store.getIsDeleted())
        .createdAt(store.getCreatedAt())
        .updatedAt(store.getUpdatedAt())
        .build();
  }
}
