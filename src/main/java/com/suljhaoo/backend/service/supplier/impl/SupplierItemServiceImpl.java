package com.suljhaoo.backend.service.supplier.impl;

import com.suljhaoo.backend.enity.auth.Store;
import com.suljhaoo.backend.enity.stock.Stock;
import com.suljhaoo.backend.enity.stock.Supplier;
import com.suljhaoo.backend.enity.stock.SupplierItem;
import com.suljhaoo.backend.model.request.stock.CreateStockRequest;
import com.suljhaoo.backend.model.request.supplier.CreateSupplierItemRequest;
import com.suljhaoo.backend.model.request.supplier.UpdateSupplierItemRequest;
import com.suljhaoo.backend.model.response.supplier.SupplierItemListResult;
import com.suljhaoo.backend.model.response.supplier.SupplierItemResponse;
import com.suljhaoo.backend.repository.auth.StoreRepository;
import com.suljhaoo.backend.repository.stock.StockRepository;
import com.suljhaoo.backend.repository.stock.SupplierItemRepository;
import com.suljhaoo.backend.repository.stock.SupplierRepository;
import com.suljhaoo.backend.service.stock.StockService;
import com.suljhaoo.backend.service.supplier.SupplierItemService;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierItemServiceImpl implements SupplierItemService {

  private final SupplierItemRepository supplierItemRepository;
  private final SupplierRepository supplierRepository;
  private final StoreRepository storeRepository;
  private final StockRepository stockRepository;
  private final StockService stockService;

  @Override
  @Transactional
  public SupplierItemResponse createSupplierItem(
      String userId, String storeId, String supplierId, CreateSupplierItemRequest request) {
    // Validate store exists and belongs to user
    Store store =
        storeRepository
            .findById(storeId)
            .orElseThrow(() -> new RuntimeException("Store not found"));

    if (!store.getUser().getId().equals(userId)) {
      throw new RuntimeException("Store does not belong to user");
    }

    // Validate supplier exists and belongs to store
    Supplier supplier =
        supplierRepository
            .findByIdAndStore_Id(UUID.fromString(supplierId), storeId)
            .orElseThrow(() -> new RuntimeException("Supplier not found"));

    // Validate name
    if (request.getName() == null || request.getName().trim().isEmpty()) {
      throw new RuntimeException("Item name is required");
    }

    // Validate unit
    if (request.getUnit() == null || request.getUnit().trim().isEmpty()) {
      throw new RuntimeException("Unit is required");
    }

    // Validate category
    if (request.getCategory() == null || request.getCategory().trim().isEmpty()) {
      throw new RuntimeException("Category is required");
    }

    String trimmedName = request.getName().trim();

    // Check if item with same name already exists for this supplier (case-insensitive)
    if (supplierItemRepository.existsBySupplier_IdAndStore_IdAndNameIgnoreCase(
        supplier.getId(), storeId, trimmedName)) {
      throw new RuntimeException(
          "Item \""
              + trimmedName
              + "\" already exists in the orderable items list for this supplier");
    }

    UUID finalStockItemId = null;

    // If stockItemId is provided, validate it
    if (request.getStockItemId() != null && !request.getStockItemId().trim().isEmpty()) {
      try {
        UUID stockItemUuid = UUID.fromString(request.getStockItemId());
        Optional<Stock> stockItem = stockRepository.findByIdAndStore_Id(stockItemUuid, storeId);
        if (stockItem.isEmpty()) {
          throw new RuntimeException("Stock item not found or does not belong to store");
        }
        finalStockItemId = stockItemUuid;
      } catch (IllegalArgumentException e) {
        throw new RuntimeException("Invalid stock item ID format");
      }
    } else {
      // If stockItemId is NOT provided, check if stock item with same name exists
      Optional<Stock> existingStockItem =
          stockRepository.findByStore_IdAndName(storeId, trimmedName);

      if (existingStockItem.isPresent()) {
        // Stock item with same name exists, use its ID
        finalStockItemId = existingStockItem.get().getId();
        log.info(
            "Found existing stock item \"{}\" ({}), linking supplier item to it",
            trimmedName,
            finalStockItemId);
      } else {
        // Stock item with same name doesn't exist, create a new one
        try {
          CreateStockRequest createStockRequest =
              CreateStockRequest.builder()
                  .name(trimmedName)
                  .quantity(request.getQuantity() != null ? request.getQuantity() : BigDecimal.ZERO)
                  .minLevel(request.getMinLevel() != null ? request.getMinLevel() : BigDecimal.ZERO)
                  .unit(request.getUnit().trim())
                  .category(request.getCategory().trim())
                  .description(
                      request.getDescription() != null ? request.getDescription().trim() : null)
                  .supplierId(supplier.getId())
                  .supplierName(supplier.getName())
                  .build();

          var newStockItem = stockService.createStock(userId, storeId, createStockRequest);
          finalStockItemId = newStockItem.getId();
          log.info(
              "Created new stock item \"{}\" ({}) and linking supplier item to it",
              trimmedName,
              finalStockItemId);
        } catch (Exception e) {
          log.error(
              "Error creating stock item for supplier item \"{}\": {}",
              trimmedName,
              e.getMessage());
          throw new RuntimeException("Failed to create stock item: " + e.getMessage());
        }
      }
    }

    // Create supplier item
    SupplierItem supplierItem =
        SupplierItem.builder()
            .supplier(supplier)
            .store(store)
            .name(trimmedName)
            .unit(request.getUnit().trim())
            .category(request.getCategory().trim())
            .isDirty(false)
            .build();

    // Link to stock item if available
    if (finalStockItemId != null) {
      Stock stock = stockRepository.findById(finalStockItemId).orElse(null);
      supplierItem.setStock(stock);
    }

    supplierItem = supplierItemRepository.saveAndFlush(supplierItem);

    log.info(
        "Supplier item created: {} for supplier: {} by user: {}",
        supplierItem.getId(),
        supplierId,
        userId);

    return mapToResponse(supplierItem, userId);
  }

  @Override
  public SupplierItemListResult getAllSupplierItems(
      String userId, String storeId, String supplierId, Integer limit, Integer skip) {
    // Validate store exists and belongs to user
    Store store =
        storeRepository
            .findById(storeId)
            .orElseThrow(() -> new RuntimeException("Store not found"));

    if (!store.getUser().getId().equals(userId)) {
      throw new RuntimeException("Store does not belong to user");
    }

    // Validate supplier exists and belongs to store
    Supplier supplier =
        supplierRepository
            .findByIdAndStore_Id(UUID.fromString(supplierId), storeId)
            .orElseThrow(() -> new RuntimeException("Supplier not found"));

    int pageSize = limit != null && limit > 0 ? limit : 100;
    int pageNumber = skip != null && skip >= 0 ? skip / pageSize : 0;
    Pageable pageable = PageRequest.of(pageNumber, pageSize);

    Page<SupplierItem> itemsPage =
        supplierItemRepository.findBySupplier_IdAndStore_IdOrderByCreatedAtDesc(
            supplier.getId(), storeId, pageable);

    List<SupplierItemResponse> items =
        itemsPage.getContent().stream()
            .map(item -> mapToResponse(item, userId))
            .collect(Collectors.toList());

    return SupplierItemListResult.builder()
        .items(items)
        .total(itemsPage.getTotalElements())
        .build();
  }

  @Override
  @Transactional
  public SupplierItemResponse updateSupplierItem(
      String itemId,
      String userId,
      String storeId,
      String supplierId,
      UpdateSupplierItemRequest request) {
    // Find supplier item by ID, supplier ID, and store ID
    SupplierItem supplierItem =
        supplierItemRepository
            .findByIdAndSupplier_IdAndStore_Id(
                UUID.fromString(itemId), UUID.fromString(supplierId), storeId)
            .orElseThrow(() -> new RuntimeException("Supplier item not found"));

    // Update name if provided
    if (request.getName() != null) {
      String trimmedName = request.getName().trim();
      if (trimmedName.isEmpty()) {
        throw new RuntimeException("Item name cannot be empty");
      }
      supplierItem.setName(trimmedName);
    }

    // Update unit if provided
    if (request.getUnit() != null) {
      String trimmedUnit = request.getUnit().trim();
      if (trimmedUnit.isEmpty()) {
        throw new RuntimeException("Unit cannot be empty");
      }
      supplierItem.setUnit(trimmedUnit);
    }

    // Update category if provided
    if (request.getCategory() != null) {
      String trimmedCategory = request.getCategory().trim();
      if (trimmedCategory.isEmpty()) {
        throw new RuntimeException("Category cannot be empty");
      }
      supplierItem.setCategory(trimmedCategory);
    }

    supplierItem = supplierItemRepository.save(supplierItem);

    log.info("Supplier item updated: {} for supplier: {} by user: {}", itemId, supplierId, userId);

    return mapToResponse(supplierItem, userId);
  }

  @Override
  @Transactional
  public void deleteSupplierItem(String itemId, String userId, String storeId, String supplierId) {
    SupplierItem supplierItem =
        supplierItemRepository
            .findByIdAndSupplier_IdAndStore_Id(
                UUID.fromString(itemId), UUID.fromString(supplierId), storeId)
            .orElseThrow(() -> new RuntimeException("Supplier item not found"));

    supplierItemRepository.delete(supplierItem);

    log.info("Supplier item deleted: {} for supplier: {} by user: {}", itemId, supplierId, userId);
  }

  private SupplierItemResponse mapToResponse(SupplierItem supplierItem, String userId) {
    return SupplierItemResponse.builder()
        .id(supplierItem.getId())
        .userId(userId)
        .supplierId(
            supplierItem.getSupplierId() != null ? supplierItem.getSupplierId().toString() : null)
        .storeId(supplierItem.getStoreId())
        .stockItemId(
            supplierItem.getStockItemId() != null ? supplierItem.getStockItemId().toString() : null)
        .name(supplierItem.getName())
        .unit(supplierItem.getUnit())
        .category(supplierItem.getCategory())
        .createdAt(supplierItem.getCreatedAt())
        .updatedAt(supplierItem.getUpdatedAt())
        .build();
  }
}
