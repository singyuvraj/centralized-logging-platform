package com.suljhaoo.backend.service.sales;

import com.suljhaoo.backend.model.request.sales.CreateSaleRequest;
import com.suljhaoo.backend.model.request.sales.UpdateSaleRequest;
import com.suljhaoo.backend.model.response.sales.SaleResponse;
import com.suljhaoo.backend.model.response.sales.SalesListResult;

public interface SaleService {
  SaleResponse createSale(String userId, String storeId, CreateSaleRequest request);

  SalesListResult getSales(String userId, String storeId, Integer limit, Integer skip);

  SaleResponse getSaleById(String saleId, String userId, String storeId);

  SaleResponse updateSale(String saleId, String userId, String storeId, UpdateSaleRequest request);

  void deleteSale(String saleId, String userId, String storeId);

  SalesListResult getCashSales(String userId, String storeId, Integer limit, Integer skip);
}
