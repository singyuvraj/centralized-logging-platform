package com.suljhaoo.backend.service.supplier;

import com.suljhaoo.backend.model.request.supplier.CreateSupplierRequest;
import com.suljhaoo.backend.model.request.supplier.UpdateSupplierRequest;
import com.suljhaoo.backend.model.response.supplier.SupplierListResult;
import com.suljhaoo.backend.model.response.supplier.SupplierResponse;

public interface SupplierService {
  SupplierResponse createSupplier(String userId, String storeId, CreateSupplierRequest request);

  SupplierListResult getAllSuppliers(String userId, String storeId, Integer limit, Integer skip);

  SupplierResponse getSupplierById(String supplierId, String userId, String storeId);

  SupplierResponse updateSupplier(
      String supplierId, String userId, String storeId, UpdateSupplierRequest request);

  void deleteSupplier(String supplierId, String userId, String storeId);
}
