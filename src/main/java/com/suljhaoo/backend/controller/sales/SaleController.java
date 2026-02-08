package com.suljhaoo.backend.controller.sales;

import com.suljhaoo.backend.aspect.ValidateUserAccess;
import com.suljhaoo.backend.model.request.sales.CreateSaleRequest;
import com.suljhaoo.backend.model.request.sales.UpdateSaleRequest;
import com.suljhaoo.backend.model.response.sales.SaleResponse;
import com.suljhaoo.backend.model.response.sales.SaleSingleResponse;
import com.suljhaoo.backend.model.response.sales.SalesListResponse;
import com.suljhaoo.backend.model.response.sales.SalesListResult;
import com.suljhaoo.backend.service.sales.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SaleController {

  /** Create a new sale record POST /api/sales/user/{userId}/{storeId} */
  @ValidateUserAccess
  @PostMapping("/user/{userId}/{storeId}")
  public ResponseEntity<SaleSingleResponse> createSale(
      @PathVariable String userId,
      @PathVariable String storeId,
      @Valid @RequestBody CreateSaleRequest request) {
    SaleResponse sale = saleService.createSale(userId, storeId, request);

    SaleSingleResponse response =
        SaleSingleResponse.builder()
            .status("success")
            .message("Sale created successfully")
            .data(SaleSingleResponse.SaleSingleData.builder().sale(sale).build())
            .build();

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  private final SaleService saleService;

  /** Get all sales for a specific user and store GET /api/sales/user/{userId}/{storeId} */
  @ValidateUserAccess
  @GetMapping("/user/{userId}/{storeId}")
  public ResponseEntity<SalesListResponse> getAllSales(
      @PathVariable String userId,
      @PathVariable String storeId,
      @RequestParam(required = false) Integer limit,
      @RequestParam(required = false) Integer skip) {
    SalesListResult result = saleService.getSales(userId, storeId, limit, skip);

    SalesListResponse response =
        SalesListResponse.builder()
            .status("success")
            .message("Sales retrieved successfully")
            .data(
                SalesListResponse.SalesListData.builder()
                    .sales(result.getSales())
                    .total(result.getTotal())
                    .limit(limit != null ? limit : 100)
                    .skip(skip != null ? skip : 0)
                    .build())
            .build();

    return ResponseEntity.ok(response);
  }

  /**
   * Get all cash sales for a user and store (cash, upi, card) GET
   * /api/sales/user/{userId}/{storeId}/cash-sales
   */
  @ValidateUserAccess
  @GetMapping("/user/{userId}/{storeId}/cash-sales")
  public ResponseEntity<SalesListResponse> getAllCashSales(
      @PathVariable String userId,
      @PathVariable String storeId,
      @RequestParam(required = false) Integer limit,
      @RequestParam(required = false) Integer skip) {
    SalesListResult result = saleService.getCashSales(userId, storeId, limit, skip);

    SalesListResponse response =
        SalesListResponse.builder()
            .status("success")
            .message("Cash sales retrieved successfully")
            .data(
                SalesListResponse.SalesListData.builder()
                    .sales(result.getSales())
                    .total(result.getTotal())
                    .limit(limit != null ? limit : 1000)
                    .skip(skip != null ? skip : 0)
                    .build())
            .build();

    return ResponseEntity.ok(response);
  }

  /** Get a single sale by ID GET /api/sales/user/{userId}/{storeId}/{saleId} */
  @ValidateUserAccess
  @GetMapping("/user/{userId}/{storeId}/{saleId}")
  public ResponseEntity<SaleSingleResponse> getSaleById(
      @PathVariable String userId, @PathVariable String storeId, @PathVariable String saleId) {
    SaleResponse sale = saleService.getSaleById(saleId, userId, storeId);

    SaleSingleResponse response =
        SaleSingleResponse.builder()
            .status("success")
            .message("Sale retrieved successfully")
            .data(SaleSingleResponse.SaleSingleData.builder().sale(sale).build())
            .build();

    return ResponseEntity.ok(response);
  }

  /** Update a sale record PUT /api/sales/user/{userId}/{storeId}/{saleId} */
  @ValidateUserAccess
  @PutMapping("/user/{userId}/{storeId}/{saleId}")
  public ResponseEntity<SaleSingleResponse> updateSale(
      @PathVariable String userId,
      @PathVariable String storeId,
      @PathVariable String saleId,
      @Valid @RequestBody UpdateSaleRequest request) {
    SaleResponse sale = saleService.updateSale(saleId, userId, storeId, request);

    SaleSingleResponse response =
        SaleSingleResponse.builder()
            .status("success")
            .message("Sale updated successfully")
            .data(SaleSingleResponse.SaleSingleData.builder().sale(sale).build())
            .build();

    return ResponseEntity.ok(response);
  }

  /** Delete a sale record DELETE /api/sales/user/{userId}/{storeId}/{saleId} */
  @ValidateUserAccess
  @DeleteMapping("/user/{userId}/{storeId}/{saleId}")
  public ResponseEntity<SaleSingleResponse> deleteSale(
      @PathVariable String userId, @PathVariable String storeId, @PathVariable String saleId) {
    saleService.deleteSale(saleId, userId, storeId);

    SaleSingleResponse response =
        SaleSingleResponse.builder().status("success").message("Sale deleted successfully").build();

    return ResponseEntity.ok(response);
  }
}
