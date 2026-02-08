package com.suljhaoo.backend.service.sales.impl;

import com.suljhaoo.backend.enity.auth.Store;
import com.suljhaoo.backend.enity.auth.User;
import com.suljhaoo.backend.enity.sales.Sale;
import com.suljhaoo.backend.model.request.sales.CreateSaleRequest;
import com.suljhaoo.backend.model.request.sales.UpdateSaleRequest;
import com.suljhaoo.backend.model.response.sales.SaleResponse;
import com.suljhaoo.backend.model.response.sales.SalesListResult;
import com.suljhaoo.backend.repository.auth.StoreRepository;
import com.suljhaoo.backend.repository.auth.UserRepository;
import com.suljhaoo.backend.repository.sales.SaleRepository;
import com.suljhaoo.backend.service.sales.SaleService;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
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
public class SaleServiceImpl implements SaleService {

  private final SaleRepository saleRepository;
  private final UserRepository userRepository;
  private final StoreRepository storeRepository;

  @Override
  @Transactional
  public SaleResponse createSale(String userId, String storeId, CreateSaleRequest request) {
    // Validate user exists
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

    // Validate store exists
    Store store =
        storeRepository
            .findById(storeId)
            .orElseThrow(() -> new RuntimeException("Store not found"));

    // Validate store belongs to user
    if (!store.getUser().getId().equals(userId)) {
      throw new RuntimeException("Store does not belong to user");
    }

    // Validate amount
    if (request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
      throw new RuntimeException("Amount must be greater than 0");
    }

    // Validate payment method
    String paymentMethod = request.getPaymentMethod().toLowerCase();
    if (!paymentMethod.matches("^(cash|upi|card|credit)$")) {
      throw new RuntimeException("Invalid payment method. Must be one of: cash, upi, card, credit");
    }

    // Parse and validate saleDate if provided
    LocalDateTime saleDate = request.getSaleDate();
    if (saleDate == null) {
      saleDate = LocalDateTime.now();
    } else {
      // Allow past dates but not future dates beyond reasonable limit (e.g., 1 day in future for
      // timezone issues)
      LocalDateTime maxFutureDate = LocalDateTime.now().plusDays(1);
      if (saleDate.isAfter(maxFutureDate)) {
        throw new RuntimeException("Sale date cannot be in the future");
      }
    }

    // Create sale
    Sale sale =
        Sale.builder()
            .user(user)
            .store(store)
            .amount(request.getAmount())
            .paymentMethod(paymentMethod)
            .customerName(
                request.getCustomerName() != null ? request.getCustomerName().trim() : null)
            .note(request.getNote() != null ? request.getNote().trim() : null)
            .saleDate(saleDate)
            .version(1)
            .updatedBy("web")
            .build();

    sale = saleRepository.save(sale);

    log.info("Sale created: {} by user: {}", sale.getId(), userId);

    return mapToResponse(sale);
  }

  @Override
  public SalesListResult getSales(String userId, String storeId, Integer limit, Integer skip) {
    // Validate store exists and belongs to user
    Store store =
        storeRepository
            .findById(storeId)
            .orElseThrow(() -> new RuntimeException("Store not found"));

    if (!store.getUser().getId().equals(userId)) {
      throw new RuntimeException("Store does not belong to user");
    }

    int pageSize = limit != null && limit > 0 ? limit : 100;
    int pageNumber = skip != null && skip >= 0 ? skip / pageSize : 0;
    Pageable pageable = PageRequest.of(pageNumber, pageSize);

    Page<Sale> salesPage =
        saleRepository.findByUser_IdAndStore_IdOrderBySaleDateDesc(userId, storeId, pageable);

    List<SaleResponse> sales =
        salesPage.getContent().stream().map(this::mapToResponse).collect(Collectors.toList());

    return SalesListResult.builder().sales(sales).total(salesPage.getTotalElements()).build();
  }

  @Override
  public SaleResponse getSaleById(String saleId, String userId, String storeId) {
    Sale sale =
        saleRepository
            .findByIdAndUser_IdAndStore_Id(UUID.fromString(saleId), userId, storeId)
            .orElseThrow(() -> new RuntimeException("Sale not found"));

    return mapToResponse(sale);
  }

  @Override
  @Transactional
  public SaleResponse updateSale(
      String saleId, String userId, String storeId, UpdateSaleRequest request) {
    // Find sale
    Sale sale =
        saleRepository
            .findByIdAndUser_IdAndStore_Id(UUID.fromString(saleId), userId, storeId)
            .orElseThrow(() -> new RuntimeException("Sale not found"));

    // Validate amount if provided
    if (request.getAmount() != null) {
      if (request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
        throw new RuntimeException("Amount must be greater than 0");
      }
      sale.setAmount(request.getAmount());
    }

    // Validate and update payment method if provided
    if (request.getPaymentMethod() != null) {
      String paymentMethod = request.getPaymentMethod().toLowerCase();
      if (!paymentMethod.matches("^(cash|upi|card|credit)$")) {
        throw new RuntimeException(
            "Invalid payment method. Must be one of: cash, upi, card, credit");
      }
      sale.setPaymentMethod(paymentMethod);
    }

    // Update customer name if provided
    if (request.getCustomerName() != null) {
      sale.setCustomerName(request.getCustomerName().trim());
    }

    // Update note if provided
    if (request.getNote() != null) {
      sale.setNote(request.getNote().trim());
    }

    // Parse and validate saleDate if provided
    if (request.getSaleDate() != null) {
      LocalDateTime saleDate = request.getSaleDate();
      // Allow past dates but not future dates beyond reasonable limit
      LocalDateTime maxFutureDate = LocalDateTime.now().plusDays(1);
      if (saleDate.isAfter(maxFutureDate)) {
        throw new RuntimeException("Sale date cannot be in the future");
      }
      sale.setSaleDate(saleDate);
    }

    // Increment version and set updated_by
    sale.setVersion(sale.getVersion() + 1);
    sale.setUpdatedBy("web");

    sale = saleRepository.save(sale);

    log.info("Sale updated: {} for user: {}", saleId, userId);

    return mapToResponse(sale);
  }

  @Override
  @Transactional
  public void deleteSale(String saleId, String userId, String storeId) {
    Sale sale =
        saleRepository
            .findByIdAndUser_IdAndStore_Id(UUID.fromString(saleId), userId, storeId)
            .orElseThrow(() -> new RuntimeException("Sale not found"));

    saleRepository.delete(sale);

    log.info("Sale deleted: {} for user: {}", saleId, userId);
  }

  @Override
  public SalesListResult getCashSales(String userId, String storeId, Integer limit, Integer skip) {
    // Validate store exists and belongs to user
    Store store =
        storeRepository
            .findById(storeId)
            .orElseThrow(() -> new RuntimeException("Store not found"));

    if (!store.getUser().getId().equals(userId)) {
      throw new RuntimeException("Store does not belong to user");
    }

    int pageSize = limit != null && limit > 0 ? limit : 1000;
    int pageNumber = skip != null && skip >= 0 ? skip / pageSize : 0;
    Pageable pageable = PageRequest.of(pageNumber, pageSize);

    Page<Sale> salesPage = saleRepository.findCashSalesByUserAndStore(userId, storeId, pageable);

    List<SaleResponse> sales =
        salesPage.getContent().stream().map(this::mapToResponse).collect(Collectors.toList());

    return SalesListResult.builder().sales(sales).total(salesPage.getTotalElements()).build();
  }

  private SaleResponse mapToResponse(Sale sale) {
    return SaleResponse.builder()
        .id(sale.getId().toString())
        .userId(sale.getUserId())
        .storeId(sale.getStoreId())
        .amount(sale.getAmount())
        .paymentMethod(sale.getPaymentMethod())
        .customerName(sale.getCustomerName())
        .note(sale.getNote())
        .saleDate(sale.getSaleDate())
        .createdAt(sale.getCreatedAt())
        .updatedAt(sale.getUpdatedAt())
        .version(sale.getVersion())
        .updatedBy(sale.getUpdatedBy())
        .build();
  }
}
