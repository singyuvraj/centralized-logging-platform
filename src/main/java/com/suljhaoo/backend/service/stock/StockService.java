package com.suljhaoo.backend.service.stock;

import com.suljhaoo.backend.model.request.stock.CreateStockRequest;
import com.suljhaoo.backend.model.request.stock.UpdateStockRequest;
import com.suljhaoo.backend.model.response.stock.BulkUploadResult;
import com.suljhaoo.backend.model.response.stock.StockListResult;
import com.suljhaoo.backend.model.response.stock.StockResponse;
import org.springframework.web.multipart.MultipartFile;

public interface StockService {
  StockResponse createStock(String userId, String storeId, CreateStockRequest request);

  StockListResult getAllStocks(String userId, String storeId, Integer limit, Integer skip);

  StockListResult getLowStockItems(String userId, String storeId, Integer limit, Integer skip);

  StockResponse updateStock(
      String stockId, String userId, String storeId, UpdateStockRequest request);

  void deleteStock(String stockId, String userId, String storeId);

  BulkUploadResult bulkUploadStocks(String userId, String storeId, MultipartFile file);
}
