package com.suljhaoo.backend.controller.supplier;

import com.suljhaoo.backend.aspect.ValidateUserAccess;
import com.suljhaoo.backend.model.request.supplier.CreateSupplierItemRequest;
import com.suljhaoo.backend.model.request.supplier.CreateSupplierRequest;
import com.suljhaoo.backend.model.request.supplier.UpdateSupplierItemRequest;
import com.suljhaoo.backend.model.request.supplier.UpdateSupplierRequest;
import com.suljhaoo.backend.model.response.supplier.SupplierItemListResponse;
import com.suljhaoo.backend.model.response.supplier.SupplierItemSingleResponse;
import com.suljhaoo.backend.model.response.supplier.SupplierListResponse;
import com.suljhaoo.backend.model.response.supplier.SupplierResponse;
import com.suljhaoo.backend.model.response.supplier.SupplierSingleResponse;
import com.suljhaoo.backend.service.supplier.SupplierItemService;
import com.suljhaoo.backend.service.supplier.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/suppliers")
@RequiredArgsConstructor
public class SupplierController {

  private final SupplierService supplierService;
  private final SupplierItemService supplierItemService;

  /** Create a new supplier record POST /api/suppliers/user/{userId}/{storeId} */
  @ValidateUserAccess
  @PostMapping("/user/{userId}/{storeId}")
  public ResponseEntity<SupplierSingleResponse> createSupplier(
      @PathVariable String userId,
      @PathVariable String storeId,
      @Valid @RequestBody CreateSupplierRequest request) {
    SupplierResponse supplier = supplierService.createSupplier(userId, storeId, request);

    SupplierSingleResponse response =
        SupplierSingleResponse.builder()
            .status("success")
            .message("Supplier created successfully")
            .data(SupplierSingleResponse.SupplierSingleData.builder().supplier(supplier).build())
            .build();

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /** Get all suppliers for a specific user and store GET /api/suppliers/user/{userId}/{storeId} */
  @ValidateUserAccess
  @GetMapping("/user/{userId}/{storeId}")
  public ResponseEntity<SupplierListResponse> getAllSuppliers(
      @PathVariable String userId,
      @PathVariable String storeId,
      @RequestParam(required = false) Integer limit,
      @RequestParam(required = false) Integer skip) {
    var result = supplierService.getAllSuppliers(userId, storeId, limit, skip);

    SupplierListResponse response =
        SupplierListResponse.builder()
            .status("success")
            .message("Suppliers retrieved successfully")
            .data(
                SupplierListResponse.SupplierListData.builder()
                    .suppliers(result.getSuppliers())
                    .total(result.getTotal())
                    .limit(limit != null ? limit : 100)
                    .skip(skip != null ? skip : 0)
                    .build())
            .build();

    return ResponseEntity.ok(response);
  }

  /** Get a single supplier by ID GET /api/suppliers/user/{userId}/{storeId}/{id} */
  @ValidateUserAccess
  @GetMapping("/user/{userId}/{storeId}/{id}")
  public ResponseEntity<SupplierSingleResponse> getSupplierById(
      @PathVariable String userId, @PathVariable String storeId, @PathVariable String id) {
    SupplierResponse supplier = supplierService.getSupplierById(id, userId, storeId);

    SupplierSingleResponse response =
        SupplierSingleResponse.builder()
            .status("success")
            .message("Supplier retrieved successfully")
            .data(SupplierSingleResponse.SupplierSingleData.builder().supplier(supplier).build())
            .build();

    return ResponseEntity.ok(response);
  }

  /** Update a supplier record PUT /api/suppliers/user/{userId}/{storeId}/{id} */
  @ValidateUserAccess
  @PutMapping("/user/{userId}/{storeId}/{id}")
  public ResponseEntity<SupplierSingleResponse> updateSupplier(
      @PathVariable String userId,
      @PathVariable String storeId,
      @PathVariable String id,
      @Valid @RequestBody UpdateSupplierRequest request) {
    SupplierResponse supplier = supplierService.updateSupplier(id, userId, storeId, request);

    SupplierSingleResponse response =
        SupplierSingleResponse.builder()
            .status("success")
            .message("Supplier updated successfully")
            .data(SupplierSingleResponse.SupplierSingleData.builder().supplier(supplier).build())
            .build();

    return ResponseEntity.ok(response);
  }

  /** Delete a supplier record DELETE /api/suppliers/user/{userId}/{storeId}/{id} */
  @ValidateUserAccess
  @DeleteMapping("/user/{userId}/{storeId}/{id}")
  public ResponseEntity<SupplierSingleResponse> deleteSupplier(
      @PathVariable String userId, @PathVariable String storeId, @PathVariable String id) {
    supplierService.deleteSupplier(id, userId, storeId);

    SupplierSingleResponse response =
        SupplierSingleResponse.builder()
            .status("success")
            .message("Supplier deleted successfully")
            .build();

    return ResponseEntity.ok(response);
  }

  /** Create a new supplier item POST /api/suppliers/user/{userId}/{storeId}/{supplierId}/items */
  @ValidateUserAccess
  @PostMapping("/user/{userId}/{storeId}/{supplierId}/items")
  public ResponseEntity<SupplierItemSingleResponse> createSupplierItem(
      @PathVariable String userId,
      @PathVariable String storeId,
      @PathVariable String supplierId,
      @Valid @RequestBody CreateSupplierItemRequest request) {
    var item = supplierItemService.createSupplierItem(userId, storeId, supplierId, request);

    SupplierItemSingleResponse response =
        SupplierItemSingleResponse.builder()
            .status("success")
            .message("Supplier item created successfully")
            .data(SupplierItemSingleResponse.SupplierItemSingleData.builder().item(item).build())
            .build();

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /** Get all supplier items GET /api/suppliers/user/{userId}/{storeId}/{supplierId}/items */
  @ValidateUserAccess
  @GetMapping("/user/{userId}/{storeId}/{supplierId}/items")
  public ResponseEntity<SupplierItemListResponse> getAllSupplierItems(
      @PathVariable String userId,
      @PathVariable String storeId,
      @PathVariable String supplierId,
      @RequestParam(required = false) Integer limit,
      @RequestParam(required = false) Integer skip) {
    var result = supplierItemService.getAllSupplierItems(userId, storeId, supplierId, limit, skip);

    SupplierItemListResponse response =
        SupplierItemListResponse.builder()
            .status("success")
            .message("Supplier items retrieved successfully")
            .data(
                SupplierItemListResponse.SupplierItemListData.builder()
                    .items(result.getItems())
                    .total(result.getTotal())
                    .limit(limit != null ? limit : 100)
                    .skip(skip != null ? skip : 0)
                    .build())
            .build();

    return ResponseEntity.ok(response);
  }

  /**
   * Update a supplier item PUT /api/suppliers/user/{userId}/{storeId}/{supplierId}/items/{itemId}
   */
  @ValidateUserAccess
  @PutMapping("/user/{userId}/{storeId}/{supplierId}/items/{itemId}")
  public ResponseEntity<SupplierItemSingleResponse> updateSupplierItem(
      @PathVariable String userId,
      @PathVariable String storeId,
      @PathVariable String supplierId,
      @PathVariable String itemId,
      @Valid @RequestBody UpdateSupplierItemRequest request) {
    var item = supplierItemService.updateSupplierItem(itemId, userId, storeId, supplierId, request);

    SupplierItemSingleResponse response =
        SupplierItemSingleResponse.builder()
            .status("success")
            .message("Supplier item updated successfully")
            .data(SupplierItemSingleResponse.SupplierItemSingleData.builder().item(item).build())
            .build();

    return ResponseEntity.ok(response);
  }

  /**
   * Delete a supplier item DELETE
   * /api/suppliers/user/{userId}/{storeId}/{supplierId}/items/{itemId}
   */
  @ValidateUserAccess
  @DeleteMapping("/user/{userId}/{storeId}/{supplierId}/items/{itemId}")
  public ResponseEntity<SupplierItemSingleResponse> deleteSupplierItem(
      @PathVariable String userId,
      @PathVariable String storeId,
      @PathVariable String supplierId,
      @PathVariable String itemId) {
    supplierItemService.deleteSupplierItem(itemId, userId, storeId, supplierId);

    SupplierItemSingleResponse response =
        SupplierItemSingleResponse.builder()
            .status("success")
            .message("Supplier item deleted successfully")
            .build();

    return ResponseEntity.ok(response);
  }
}
