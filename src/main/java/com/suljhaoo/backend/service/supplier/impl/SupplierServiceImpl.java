package com.suljhaoo.backend.service.supplier.impl;

import com.suljhaoo.backend.enity.auth.Store;
import com.suljhaoo.backend.enity.stock.Supplier;
import com.suljhaoo.backend.model.request.supplier.CreateSupplierRequest;
import com.suljhaoo.backend.model.request.supplier.UpdateSupplierRequest;
import com.suljhaoo.backend.model.response.supplier.SupplierListResult;
import com.suljhaoo.backend.model.response.supplier.SupplierResponse;
import com.suljhaoo.backend.repository.auth.StoreRepository;
import com.suljhaoo.backend.repository.auth.UserRepository;
import com.suljhaoo.backend.repository.stock.SupplierRepository;
import com.suljhaoo.backend.service.supplier.SupplierService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
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
public class SupplierServiceImpl implements SupplierService {

  private final SupplierRepository supplierRepository;
  private final UserRepository userRepository;
  private final StoreRepository storeRepository;

  @Override
  @Transactional
  public SupplierResponse createSupplier(
      String userId, String storeId, CreateSupplierRequest request) {
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

    // Validate supplierType
    String supplierType = validateAndExtractSupplierType(request);
    String finalNickName = extractNickNameDetails(request, supplierType);
    String finalBrands = request.getBrands() != null ? request.getBrands().trim() : null;
    String finalSalesman = request.getSalesman() != null ? request.getSalesman().trim() : null;

    // Create supplier
    Supplier supplier =
        Supplier.builder()
            .store(store)
            .supplierType(supplierType)
            .name(request.getName().trim())
            .nickName(finalNickName)
            .phone(request.getPhone().trim())
            .gstin(request.getGstin() != null ? request.getGstin().trim() : null)
            .address(request.getAddress() != null ? request.getAddress().trim() : null)
            .email(request.getEmail() != null ? request.getEmail().trim() : null)
            .brands(finalBrands)
            .salesman(finalSalesman)
            .salesPersonPhone(
                request.getSalesPersonPhone() != null ? request.getSalesPersonPhone().trim() : null)
            .distributorName(request.getBrandName() != null ? request.getBrandName().trim() : null)
            .maxNoOfCreditBills(request.getMaxNoOfCreditBills())
            .maxCreditPeriod(
                request.getMaxCreditPeriod() != null
                        && !request.getMaxCreditPeriod().trim().isEmpty()
                    ? request.getMaxCreditPeriod().trim()
                    : "EOM")
            .isDirty(false)
            .build();

    supplier = supplierRepository.saveAndFlush(supplier);

    log.info("Supplier created: {} by user: {} for store: {}", supplier.getId(), userId, storeId);

    return mapToResponse(supplier, userId);
  }

  private String validateAndExtractSupplierType(CreateSupplierRequest request) {
    return Optional.ofNullable(request.getSupplierType())
        .filter(Predicate.not(String::isBlank))
        .map(String::toLowerCase)
        .filter(
            supplierType_ -> "distributor".equals(supplierType_) || "mahajan".equals(supplierType_))
        .orElseThrow(() -> new RuntimeException("Invalid Supplier Type Provided"));
  }

  private String extractNickNameDetails(CreateSupplierRequest request, String supplierType) {

    // Validate name
    if (request.getName() == null || request.getName().trim().isEmpty()) {
      throw new RuntimeException("Supplier name is required");
    }

    // Validate type-specific fields (matching mobile app logic)
    // Mobile app: nickname/salesPersonName are optional, fallback to name if empty
    return getFinalNickName(request, supplierType);
  }

  private String getFinalNickName(CreateSupplierRequest request, String supplierType) {
    String nickName;
    if (supplierType.equals("mahajan")) {
      // For Mahajan, use nickname, fallback to name if nickname is empty
      if (request.getNickname() != null && !request.getNickname().trim().isEmpty()) {
        nickName = request.getNickname().trim();
      } else {
        nickName = request.getName().trim(); // Fallback to name if nickname is empty
      }
    } else {
      // For Distributor, use salesPersonName, fallback to name if salesPersonName is empty
      if (request.getSalesPersonName() != null && !request.getSalesPersonName().trim().isEmpty()) {
        nickName = request.getSalesPersonName().trim();
      } else {
        nickName = request.getName().trim(); // Fallback to name if salesPersonName is empty
      }
    }

    // Validate phone
    if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
      throw new RuntimeException("Phone number is required");
    }

    return nickName;
  }

  @Override
  public SupplierListResult getAllSuppliers(
      String userId, String storeId, Integer limit, Integer skip) {
    // Validate store exists and belongs to user
    Store store =
        storeRepository
            .findById(storeId)
            .orElseThrow(() -> new RuntimeException("Store not found"));

    if (!store.getUser().getId().equals(userId)) {
      throw new RuntimeException("Store does not belong to user");
    }

    int pageSize = limit != null && limit > 0 ? limit : 100; // Default 100 to match Node.js backend
    int pageNumber = skip != null && skip >= 0 ? skip / pageSize : 0;
    Pageable pageable = PageRequest.of(pageNumber, pageSize);

    Page<Supplier> suppliersPage =
        supplierRepository.findByStore_IdOrderByCreatedAtDesc(storeId, pageable);

    List<SupplierResponse> suppliers =
        suppliersPage.getContent().stream()
            .map(supplier -> mapToResponse(supplier, userId))
            .collect(Collectors.toList());

    return SupplierListResult.builder()
        .suppliers(suppliers)
        .total(suppliersPage.getTotalElements())
        .build();
  }

  @Override
  public SupplierResponse getSupplierById(String supplierId, String userId, String storeId) {
    // Validate store exists and belongs to user
    Store store =
        storeRepository
            .findById(storeId)
            .orElseThrow(() -> new RuntimeException("Store not found"));

    if (!store.getUser().getId().equals(userId)) {
      throw new RuntimeException("Store does not belong to user");
    }

    // Find supplier by ID, user ID, and store ID
    Supplier supplier =
        supplierRepository
            .findByIdAndStore_User_IdAndStore_Id(UUID.fromString(supplierId), userId, storeId)
            .orElseThrow(() -> new RuntimeException("Supplier not found"));

    return mapToResponse(supplier, userId);
  }

  @Override
  @Transactional
  public SupplierResponse updateSupplier(
      String supplierId, String userId, String storeId, UpdateSupplierRequest request) {
    // Find supplier by ID, user ID, and store ID
    Supplier supplier =
        supplierRepository
            .findByIdAndStore_User_IdAndStore_Id(UUID.fromString(supplierId), userId, storeId)
            .orElseThrow(() -> new RuntimeException("Supplier not found"));

    // Update name if provided
    if (request.getName() != null) {
      String trimmedName = request.getName().trim();
      if (trimmedName.isEmpty()) {
        throw new RuntimeException("Supplier name cannot be empty");
      }
      supplier.setName(trimmedName);
    }

    // Update supplierType if provided (affects nickName field mapping)
    String supplierType = request.getSupplierType();
    if (supplierType != null && !supplierType.trim().isEmpty()) {
      supplierType = supplierType.toLowerCase();
      if (!supplierType.equals("mahajan") && !supplierType.equals("distributor")) {
        throw new RuntimeException("Supplier type must be either 'mahajan' or 'distributor'");
      }
      supplier.setSupplierType(supplierType);

      // Update nickName based on supplier type (matching mobile app logic)
      // Mobile app: nickname/salesPersonName are optional, fallback to name if empty
      if (supplierType.equals("mahajan")) {
        if (request.getNickname() != null && !request.getNickname().trim().isEmpty()) {
          supplier.setNickName(request.getNickname().trim());
        } else {
          // Fallback to name if nickname is empty (matching mobile app behavior)
          supplier.setNickName(supplier.getName());
        }
      } else {
        if (request.getSalesPersonName() != null
            && !request.getSalesPersonName().trim().isEmpty()) {
          supplier.setNickName(request.getSalesPersonName().trim());
        } else {
          // Fallback to name if salesPersonName is empty (matching mobile app behavior)
          supplier.setNickName(supplier.getName());
        }
      }
    } else {
      // Update nickName from nickname/salesPersonName if provided
      if (request.getNickname() != null && !request.getNickname().trim().isEmpty()) {
        supplier.setNickName(request.getNickname().trim());
      }
      if (request.getSalesPersonName() != null && !request.getSalesPersonName().trim().isEmpty()) {
        supplier.setNickName(request.getSalesPersonName().trim());
      }
    }

    // Update phone if provided
    if (request.getPhone() != null) {
      String trimmedPhone = request.getPhone().trim();
      if (trimmedPhone.isEmpty()) {
        throw new RuntimeException("Phone number cannot be empty");
      }
      supplier.setPhone(trimmedPhone);
    }

    // Update optional fields
    if (request.getGstin() != null) {
      supplier.setGstin(request.getGstin().trim().isEmpty() ? null : request.getGstin().trim());
    }
    if (request.getAddress() != null) {
      supplier.setAddress(
          request.getAddress().trim().isEmpty() ? null : request.getAddress().trim());
    }
    if (request.getEmail() != null) {
      supplier.setEmail(request.getEmail().trim().isEmpty() ? null : request.getEmail().trim());
    }
    if (request.getBrands() != null) {
      supplier.setBrands(request.getBrands().trim().isEmpty() ? null : request.getBrands().trim());
    }
    if (request.getSalesman() != null) {
      supplier.setSalesman(
          request.getSalesman().trim().isEmpty() ? null : request.getSalesman().trim());
    }
    if (request.getSalesPersonPhone() != null) {
      supplier.setSalesPersonPhone(
          request.getSalesPersonPhone().trim().isEmpty()
              ? null
              : request.getSalesPersonPhone().trim());
    }
    if (request.getBrandName() != null) {
      supplier.setDistributorName(
          request.getBrandName().trim().isEmpty() ? null : request.getBrandName().trim());
    }

    // Update credit management fields
    if (request.getMaxNoOfCreditBills() != null) {
      supplier.setMaxNoOfCreditBills(request.getMaxNoOfCreditBills());
    }
    if (request.getMaxCreditPeriod() != null && !request.getMaxCreditPeriod().trim().isEmpty()) {
      supplier.setMaxCreditPeriod(request.getMaxCreditPeriod().trim());
    }

    supplier = supplierRepository.save(supplier);

    log.info("Supplier updated: {} for user: {}", supplierId, userId);

    return mapToResponse(supplier, userId);
  }

  @Override
  @Transactional
  public void deleteSupplier(String supplierId, String userId, String storeId) {
    Supplier supplier =
        supplierRepository
            .findByIdAndStore_User_IdAndStore_Id(UUID.fromString(supplierId), userId, storeId)
            .orElseThrow(() -> new RuntimeException("Supplier not found"));

    supplierRepository.delete(supplier);

    log.info("Supplier deleted: {} for user: {}", supplierId, userId);
  }

  private SupplierResponse mapToResponse(Supplier supplier, String userId) {
    // Get supplierType directly from database (no inference needed)
    String supplierType =
        supplier.getSupplierType() != null ? supplier.getSupplierType() : "distributor";

    // Map fields for backward compatibility
    String nickname = null;
    String brandName = supplier.getDistributorName();
    if (supplierType.equals("mahajan")) {
      nickname = supplier.getNickName();
    }

    return SupplierResponse.builder()
        .id(supplier.getId())
        .userId(userId)
        .storeId(supplier.getStoreId())
        .supplierType(supplier.getSupplierType())
        .name(supplier.getName())
        .nickName(supplier.getNickName())
        .phone(supplier.getPhone())
        .gstin(supplier.getGstin())
        .address(supplier.getAddress())
        .email(supplier.getEmail())
        .brands(supplier.getBrands())
        .salesman(supplier.getSalesman())
        .salesPersonPhone(supplier.getSalesPersonPhone())
        .distributorName(supplier.getDistributorName())
        .maxNoOfCreditBills(supplier.getMaxNoOfCreditBills())
        .maxCreditPeriod(supplier.getMaxCreditPeriod())
        .nickname(nickname)
        .brandName(brandName)
        .createdAt(supplier.getCreatedAt())
        .updatedAt(supplier.getUpdatedAt())
        .build();
  }
}
