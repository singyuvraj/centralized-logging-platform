package com.suljhaoo.backend.service.supplier;

import com.suljhaoo.backend.model.request.supplier.CreateSupplierItemRequest;
import com.suljhaoo.backend.model.request.supplier.UpdateSupplierItemRequest;
import com.suljhaoo.backend.model.response.supplier.SupplierItemListResult;
import com.suljhaoo.backend.model.response.supplier.SupplierItemResponse;

public interface SupplierItemService {
  SupplierItemResponse createSupplierItem(
      String userId, String storeId, String supplierId, CreateSupplierItemRequest request);

  SupplierItemListResult getAllSupplierItems(
      String userId, String storeId, String supplierId, Integer limit, Integer skip);

  SupplierItemResponse updateSupplierItem(
      String itemId,
      String userId,
      String storeId,
      String supplierId,
      UpdateSupplierItemRequest request);

  void deleteSupplierItem(String itemId, String userId, String storeId, String supplierId);
}
