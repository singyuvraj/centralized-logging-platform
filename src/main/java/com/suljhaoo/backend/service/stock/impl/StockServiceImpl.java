package com.suljhaoo.backend.service.stock.impl;

import com.suljhaoo.backend.enity.auth.Store;
import com.suljhaoo.backend.enity.auth.User;
import com.suljhaoo.backend.enity.stock.Stock;
import com.suljhaoo.backend.enity.stock.Supplier;
import com.suljhaoo.backend.model.request.stock.CreateStockRequest;
import com.suljhaoo.backend.model.request.stock.UpdateStockRequest;
import com.suljhaoo.backend.model.response.stock.BulkUploadResult;
import com.suljhaoo.backend.model.response.stock.StockListResult;
import com.suljhaoo.backend.model.response.stock.StockResponse;
import com.suljhaoo.backend.repository.auth.StoreRepository;
import com.suljhaoo.backend.repository.auth.UserRepository;
import com.suljhaoo.backend.repository.stock.StockRepository;
import com.suljhaoo.backend.repository.stock.SupplierRepository;
import com.suljhaoo.backend.service.stock.StockService;
import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockServiceImpl implements StockService {

  private final StockRepository stockRepository;
  private final UserRepository userRepository;
  private final StoreRepository storeRepository;
  private final SupplierRepository supplierRepository;

  @Override
  @Transactional
  public StockResponse createStock(String userId, String storeId, CreateStockRequest request) {
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

    // Validate quantity
    if (request.getQuantity().compareTo(java.math.BigDecimal.ZERO) < 0) {
      throw new RuntimeException("Quantity cannot be negative");
    }

    // Validate minLevel
    if (request.getMinLevel().compareTo(java.math.BigDecimal.ZERO) < 0) {
      throw new RuntimeException("Minimum level cannot be negative");
    }

    // Validate unitPrice if provided
    if (request.getUnitPrice() != null
        && request.getUnitPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
      throw new RuntimeException("Unit price cannot be negative");
    }

    // Validate and load supplier if supplierId is provided
    Supplier supplier = null;
    if (request.getSupplierId() != null) {
      supplier =
          supplierRepository
              .findById(request.getSupplierId())
              .orElseThrow(() -> new RuntimeException("Supplier not found"));

      // Validate supplier belongs to the same store
      if (!supplier.getStoreId().equals(storeId)) {
        throw new RuntimeException("Supplier does not belong to the specified store");
      }
    }

    // Create stock entity
    Stock.StockBuilder stockBuilder =
        Stock.builder()
            .store(store)
            .name(request.getName().trim())
            .quantity(request.getQuantity())
            .minLevel(request.getMinLevel())
            .unit(request.getUnit() != null ? request.getUnit().trim() : null)
            .category(request.getCategory() != null ? request.getCategory().trim() : null)
            .unitPrice(request.getUnitPrice())
            .description(request.getDescription() != null ? request.getDescription().trim() : null)
            .supplier(supplier)
            .supplierName(
                request.getSupplierName() != null ? request.getSupplierName().trim() : null);

    Stock stock = stockRepository.saveAndFlush(stockBuilder.build());

    log.info("Stock created: {} by user: {} for store: {}", stock.getId(), userId, storeId);

    return mapToResponse(stock);
  }

  @Override
  public StockListResult getAllStocks(String userId, String storeId, Integer limit, Integer skip) {
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

    Page<Stock> stocksPage = stockRepository.findByStore_IdOrderByNameAsc(storeId, pageable);

    List<StockResponse> stocks =
        stocksPage.getContent().stream().map(this::mapToResponse).collect(Collectors.toList());

    return StockListResult.builder().stocks(stocks).total(stocksPage.getTotalElements()).build();
  }

  @Override
  public StockListResult getLowStockItems(
      String userId, String storeId, Integer limit, Integer skip) {
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

    Page<Stock> stocksPage = stockRepository.findLowStockItems(storeId, pageable);

    List<StockResponse> stocks =
        stocksPage.getContent().stream().map(this::mapToResponse).collect(Collectors.toList());

    return StockListResult.builder().stocks(stocks).total(stocksPage.getTotalElements()).build();
  }

  @Override
  @Transactional
  public StockResponse updateStock(
      String stockId, String userId, String storeId, UpdateStockRequest request) {
    // Find stock by ID and store ID
    Stock stock =
        stockRepository
            .findByIdAndStore_Id(UUID.fromString(stockId), storeId)
            .orElseThrow(() -> new RuntimeException("Stock item not found"));

    // Validate store exists and belongs to user
    Store store =
        storeRepository
            .findById(storeId)
            .orElseThrow(() -> new RuntimeException("Store not found"));

    if (!store.getUser().getId().equals(userId)) {
      throw new RuntimeException("Store does not belong to user");
    }

    // Update name if provided
    if (request.getName() != null) {
      String trimmedName = request.getName().trim();
      if (trimmedName.isEmpty()) {
        throw new RuntimeException("Stock item name cannot be empty");
      }
      stock.setName(trimmedName);
    }

    // Update quantity if provided
    if (request.getQuantity() != null) {
      if (request.getQuantity().compareTo(java.math.BigDecimal.ZERO) < 0) {
        throw new RuntimeException("Quantity cannot be negative");
      }
      stock.setQuantity(request.getQuantity());
    }

    // Update minLevel if provided
    if (request.getMinLevel() != null) {
      if (request.getMinLevel().compareTo(java.math.BigDecimal.ZERO) < 0) {
        throw new RuntimeException("Minimum level cannot be negative");
      }
      stock.setMinLevel(request.getMinLevel());
    }

    // Update unit if provided (allow null to clear)
    if (request.getUnit() != null) {
      stock.setUnit(request.getUnit().trim().isEmpty() ? null : request.getUnit().trim());
    }

    // Update category if provided (allow null to clear)
    if (request.getCategory() != null) {
      stock.setCategory(
          request.getCategory().trim().isEmpty() ? null : request.getCategory().trim());
    }

    // Update description if provided (allow null to clear)
    if (request.getDescription() != null) {
      stock.setDescription(
          request.getDescription().trim().isEmpty() ? null : request.getDescription().trim());
    }

    // Update unitPrice if provided
    if (request.getUnitPrice() != null) {
      if (request.getUnitPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
        throw new RuntimeException("Unit price cannot be negative");
      }
      stock.setUnitPrice(request.getUnitPrice());
    }

    // Update supplier if supplierId is provided
    if (request.getSupplierId() != null) {
      Supplier supplier =
          supplierRepository
              .findById(request.getSupplierId())
              .orElseThrow(() -> new RuntimeException("Supplier not found"));

      // Validate supplier belongs to the same store
      if (!supplier.getStoreId().equals(storeId)) {
        throw new RuntimeException("Supplier does not belong to the specified store");
      }
      stock.setSupplier(supplier);
    }

    // Update supplierName if provided (allow null to clear)
    if (request.getSupplierName() != null) {
      stock.setSupplierName(
          request.getSupplierName().trim().isEmpty() ? null : request.getSupplierName().trim());
    }

    stock = stockRepository.saveAndFlush(stock);

    log.info("Stock updated: {} by user: {} for store: {}", stockId, userId, storeId);

    return mapToResponse(stock);
  }

  @Override
  @Transactional
  public void deleteStock(String stockId, String userId, String storeId) {
    // Find stock by ID and store ID
    Stock stock =
        stockRepository
            .findByIdAndStore_Id(UUID.fromString(stockId), storeId)
            .orElseThrow(() -> new RuntimeException("Stock item not found"));

    // Validate store exists and belongs touser
    Store store =
        storeRepository
            .findById(storeId)
            .orElseThrow(() -> new RuntimeException("Store not found"));

    if (!store.getUser().getId().equals(userId)) {
      throw new RuntimeException("Store does not belong to user");
    }

    stockRepository.delete(stock);

    log.info("Stock deleted: {} by user: {} for store: {}", stockId, userId, storeId);
  }

  @Override
  @Transactional
  public BulkUploadResult bulkUploadStocks(String userId, String storeId, MultipartFile file) {
    // Validate user exists
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

    // Validate file
    if (file == null || file.isEmpty()) {
      throw new RuntimeException("Excel file is required");
    }

    String fileName = file.getOriginalFilename();
    if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
      throw new RuntimeException("Invalid file type. Please upload an Excel file (.xlsx or .xls)");
    }

    List<String> validationErrors = new ArrayList<>();
    List<String> processingErrors = new ArrayList<>();
    int createdCount = 0;
    int updatedCount = 0;

    try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
      Sheet sheet = workbook.getSheetAt(0); // Get first sheet

      if (sheet.getPhysicalNumberOfRows() <= 1) {
        throw new RuntimeException("The Excel file is empty or invalid. Please check the format.");
      }

      // Process rows (skip header row at index 0)
      for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
          continue; // Skip empty rows
        }

        try {
          // Extract cell values
          String itemName = getCellValueAsString(row.getCell(0)); // Item Name
          String currentStockStr = getCellValueAsString(row.getCell(1)); // Current Stock
          String minLevelStr = getCellValueAsString(row.getCell(2)); // Minimum Level
          String unit = getCellValueAsString(row.getCell(3)); // Unit
          String unitPriceStr = getCellValueAsString(row.getCell(4)); // Unit Price
          String category = getCellValueAsString(row.getCell(5)); // Category
          String description = getCellValueAsString(row.getCell(6)); // Description

          if (StringUtils.isEmpty(itemName)
              && StringUtils.isEmpty(currentStockStr)
              && StringUtils.isEmpty(minLevelStr)
              && StringUtils.isEmpty(unit)
              && StringUtils.isEmpty(unitPriceStr)
              && StringUtils.isEmpty(description)
              && StringUtils.isEmpty(category)) continue;

          // Validate required fields
          if (itemName == null || itemName.trim().isEmpty()) {
            validationErrors.add(String.format("Row %d: Item Name is required", rowIndex + 1));
            continue;
          }

          BigDecimal quantity;
          try {
            if (currentStockStr == null || currentStockStr.trim().isEmpty()) {
              validationErrors.add(
                  String.format("Row %d: Current Stock is required", rowIndex + 1));
              continue;
            }
            quantity = new BigDecimal(currentStockStr.trim());
            if (quantity.compareTo(BigDecimal.ZERO) < 0) {
              validationErrors.add(
                  String.format("Row %d: Current Stock must be >= 0", rowIndex + 1));
              continue;
            }
          } catch (NumberFormatException e) {
            validationErrors.add(
                String.format("Row %d: Current Stock must be a valid number", rowIndex + 1));
            continue;
          }

          BigDecimal minLevel;
          try {
            if (minLevelStr == null || minLevelStr.trim().isEmpty()) {
              validationErrors.add(
                  String.format("Row %d: Minimum Level is required", rowIndex + 1));
              continue;
            }
            minLevel = new BigDecimal(minLevelStr.trim());
            if (minLevel.compareTo(BigDecimal.ZERO) < 0) {
              validationErrors.add(
                  String.format("Row %d: Minimum Level must be >= 0", rowIndex + 1));
              continue;
            }
          } catch (NumberFormatException e) {
            validationErrors.add(
                String.format("Row %d: Minimum Level must be a valid number", rowIndex + 1));
            continue;
          }

          // Parse unit price (optional field)
          BigDecimal unitPrice = null;
          if (unitPriceStr != null && !unitPriceStr.trim().isEmpty()) {
            try {
              unitPrice = new BigDecimal(unitPriceStr.trim());
              if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                validationErrors.add(
                    String.format("Row %d: Unit Price must be >= 0", rowIndex + 1));
                continue;
              }
            } catch (NumberFormatException e) {
              validationErrors.add(
                  String.format("Row %d: Unit Price must be a valid number", rowIndex + 1));
              continue;
            }
          }

          // Process the stock item (update if exists, create if not)
          String trimmedName = itemName.trim();
          Optional<Stock> existingStock =
              stockRepository.findByStore_IdAndName(storeId, trimmedName);

          if (existingStock.isPresent()) {
            // Update existing stock
            Stock stock = existingStock.get();
            stock.setQuantity(quantity);
            stock.setMinLevel(minLevel);
            stock.setUnit(unit != null && !unit.trim().isEmpty() ? unit.trim() : null);
            stock.setUnitPrice(unitPrice);
            stock.setCategory(
                category != null && !category.trim().isEmpty() ? category.trim() : null);
            stock.setDescription(
                description != null && !description.trim().isEmpty() ? description.trim() : null);
            stockRepository.saveAndFlush(stock);
            updatedCount++;
          } else {
            // Create new stock
            Stock newStock =
                Stock.builder()
                    .store(store)
                    .name(trimmedName)
                    .quantity(quantity)
                    .minLevel(minLevel)
                    .unit(unit != null && !unit.trim().isEmpty() ? unit.trim() : null)
                    .unitPrice(unitPrice)
                    .category(
                        category != null && !category.trim().isEmpty() ? category.trim() : null)
                    .description(
                        description != null && !description.trim().isEmpty()
                            ? description.trim()
                            : null)
                    .build();
            stockRepository.saveAndFlush(newStock);
            createdCount++;
          }
        } catch (Exception e) {
          String errorMsg =
              String.format(
                  "Row %d: %s",
                  rowIndex + 1, e.getMessage() != null ? e.getMessage() : "Failed to process item");
          processingErrors.add(errorMsg);
          log.error("Error processing stock item at row {}: {}", rowIndex + 1, e.getMessage(), e);
        }
      }
    } catch (IOException e) {
      log.error("Error reading Excel file: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to read Excel file: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Error processing Excel file: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to process Excel file: " + e.getMessage(), e);
    }

    int total = createdCount + updatedCount;
    List<String> allErrors = new ArrayList<>();
    allErrors.addAll(validationErrors);
    allErrors.addAll(processingErrors);

    log.info(
        "Bulk processed {} stock items for user: {} ({} created, {} updated, {} errors)",
        total,
        userId,
        createdCount,
        updatedCount,
        allErrors.size());

    return BulkUploadResult.builder()
        .created(createdCount)
        .updated(updatedCount)
        .total(total)
        .errors(allErrors)
        .build();
  }

  private String getCellValueAsString(Cell cell) {
    if (cell == null) {
      return null;
    }

    switch (cell.getCellType()) {
      case STRING:
        return cell.getStringCellValue();
      case NUMERIC:
        // Format numeric values without decimal if it's a whole number
        double numericValue = cell.getNumericCellValue();
        if (numericValue == (long) numericValue) {
          return String.valueOf((long) numericValue);
        } else {
          return String.valueOf(numericValue);
        }
      case BOOLEAN:
        return String.valueOf(cell.getBooleanCellValue());
      case FORMULA:
        // Evaluate formula and return as string
        return cell.getStringCellValue();
      default:
        return null;
    }
  }

  private StockResponse mapToResponse(Stock stock) {
    return StockResponse.builder()
        .id(stock.getId())
        .storeId(stock.getStoreId())
        .name(stock.getName())
        .quantity(stock.getQuantity())
        .minLevel(stock.getMinLevel())
        .unit(stock.getUnit())
        .category(stock.getCategory())
        .unitPrice(stock.getUnitPrice())
        .description(stock.getDescription())
        .supplierId(stock.getSupplierId())
        .supplierName(stock.getSupplierName())
        .createdAt(stock.getCreatedAt())
        .updatedAt(stock.getUpdatedAt())
        .build();
  }
}
