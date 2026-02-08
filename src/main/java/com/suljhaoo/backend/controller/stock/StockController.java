package com.suljhaoo.backend.controller.stock;

import com.suljhaoo.backend.aspect.ValidateUserAccess;
import com.suljhaoo.backend.model.request.stock.CreateStockRequest;
import com.suljhaoo.backend.model.request.stock.UpdateStockRequest;
import com.suljhaoo.backend.model.response.stock.BulkUploadResponse;
import com.suljhaoo.backend.model.response.stock.StockListResponse;
import com.suljhaoo.backend.model.response.stock.StockResponse;
import com.suljhaoo.backend.model.response.stock.StockSingleResponse;
import com.suljhaoo.backend.service.stock.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

  private final StockService stockService;

  /** Create a new stock item POST /api/stock/user/{userId}/{storeId} */
  @ValidateUserAccess
  @PostMapping("/user/{userId}/{storeId}")
  public ResponseEntity<StockSingleResponse> createStock(
      @PathVariable String userId,
      @PathVariable String storeId,
      @Valid @RequestBody CreateStockRequest request) {
    StockResponse stock = stockService.createStock(userId, storeId, request);

    StockSingleResponse response =
        StockSingleResponse.builder()
            .status("success")
            .message("Stock item created successfully")
            .data(StockSingleResponse.StockSingleData.builder().stock(stock).build())
            .build();

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /** Get all stock items for a specific user and store GET /api/stock/user/{userId}/{storeId} */
  @ValidateUserAccess
  @GetMapping("/user/{userId}/{storeId}")
  public ResponseEntity<StockListResponse> getAllStocks(
      @PathVariable String userId,
      @PathVariable String storeId,
      @RequestParam(required = false) Integer limit,
      @RequestParam(required = false) Integer skip) {
    var result = stockService.getAllStocks(userId, storeId, limit, skip);

    StockListResponse response =
        StockListResponse.builder()
            .status("success")
            .data(
                StockListResponse.StockListData.builder()
                    .stocks(result.getStocks())
                    .total(result.getTotal())
                    .limit(limit != null ? limit : 100)
                    .skip(skip != null ? skip : 0)
                    .build())
            .build();

    return ResponseEntity.ok(response);
  }

  /**
   * Get low stock items (quantity <= minLevel) for a specific user and store GET
   * /api/stock/user/{userId}/{storeId}/low-stock
   */
  @ValidateUserAccess
  @GetMapping("/user/{userId}/{storeId}/low-stock")
  public ResponseEntity<StockListResponse> getLowStockItems(
      @PathVariable String userId,
      @PathVariable String storeId,
      @RequestParam(required = false) Integer limit,
      @RequestParam(required = false) Integer skip) {
    var result = stockService.getLowStockItems(userId, storeId, limit, skip);

    StockListResponse response =
        StockListResponse.builder()
            .status("success")
            .data(
                StockListResponse.StockListData.builder()
                    .stocks(result.getStocks())
                    .total(result.getTotal())
                    .limit(limit != null ? limit : 100)
                    .skip(skip != null ? skip : 0)
                    .build())
            .build();

    return ResponseEntity.ok(response);
  }

  /** Update a stock item PUT /api/stock/user/{userId}/{storeId}/{stockId} */
  @ValidateUserAccess
  @PutMapping("/user/{userId}/{storeId}/{stockId}")
  public ResponseEntity<StockSingleResponse> updateStock(
      @PathVariable String userId,
      @PathVariable String storeId,
      @PathVariable String stockId,
      @Valid @RequestBody UpdateStockRequest request) {
    StockResponse stock = stockService.updateStock(stockId, userId, storeId, request);

    StockSingleResponse response =
        StockSingleResponse.builder()
            .status("success")
            .message("Stock item updated successfully")
            .data(StockSingleResponse.StockSingleData.builder().stock(stock).build())
            .build();

    return ResponseEntity.ok(response);
  }

  /** Delete a stock item DELETE /api/stock/user/{userId}/{storeId}/{stockId} */
  @ValidateUserAccess
  @DeleteMapping("/user/{userId}/{storeId}/{stockId}")
  public ResponseEntity<StockSingleResponse> deleteStock(
      @PathVariable String userId, @PathVariable String storeId, @PathVariable String stockId) {
    stockService.deleteStock(stockId, userId, storeId);

    StockSingleResponse response =
        StockSingleResponse.builder()
            .status("success")
            .message("Stock item deleted successfully")
            .build();

    return ResponseEntity.ok(response);
  }

  /** Bulk upload stock items from Excel file POST /api/stock/user/{userId}/{storeId}/bulk-upload */
  @ValidateUserAccess
  @PostMapping("/user/{userId}/{storeId}/bulk-upload")
  public ResponseEntity<BulkUploadResponse> bulkUploadStocks(
      @PathVariable String userId,
      @PathVariable String storeId,
      @RequestParam("file") MultipartFile file) {
    if (file == null || file.isEmpty()) {
      BulkUploadResponse errorResponse =
          BulkUploadResponse.builder().status("error").message("Excel file is required").build();
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    var result = stockService.bulkUploadStocks(userId, storeId, file);

    // Determine status and HTTP status code
    String status;
    HttpStatus httpStatus;
    String message;

    if (result.getTotal() == 0 && !result.getErrors().isEmpty()) {
      // No items processed, all had errors
      status = "error";
      httpStatus = HttpStatus.BAD_REQUEST;
      message = "No items were processed. Please check the errors below.";
    } else if (!result.getErrors().isEmpty()) {
      // Some items processed but there were errors
      status = "partial_success";
      httpStatus = HttpStatus.OK;
      message =
          String.format(
              "Processed %d stock item(s) with %d error(s)",
              result.getTotal(), result.getErrors().size());
    } else {
      // Full success
      status = "success";
      httpStatus = HttpStatus.CREATED;
      message = String.format("Successfully processed %d stock item(s)", result.getTotal());
    }

    // Limit errors to first 20 for response
    var limitedErrors =
        result.getErrors().stream().limit(20).collect(java.util.stream.Collectors.toList());

    BulkUploadResponse response =
        BulkUploadResponse.builder()
            .status(status)
            .message(message)
            .data(
                BulkUploadResponse.BulkUploadData.builder()
                    .created(result.getCreated())
                    .updated(result.getUpdated())
                    .total(result.getTotal())
                    .errors(limitedErrors)
                    .totalErrors(result.getErrors().size())
                    .build())
            .build();

    return ResponseEntity.status(httpStatus).body(response);
  }
}
