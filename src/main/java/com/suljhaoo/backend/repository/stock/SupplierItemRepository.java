package com.suljhaoo.backend.repository.stock;

import com.suljhaoo.backend.enity.stock.SupplierItem;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierItemRepository extends JpaRepository<SupplierItem, UUID> {
  // Get all supplier items for a supplier with pagination
  Page<SupplierItem> findBySupplier_IdAndStore_IdOrderByCreatedAtDesc(
      UUID supplierId, String storeId, Pageable pageable);

  // Get a single supplier item by ID, supplier ID, and store ID
  Optional<SupplierItem> findByIdAndSupplier_IdAndStore_Id(
      UUID id, UUID supplierId, String storeId);

  // Count supplier items for a supplier
  long countBySupplier_IdAndStore_Id(UUID supplierId, String storeId);

  // Check if item with same name exists for supplier (case-insensitive)
  boolean existsBySupplier_IdAndStore_IdAndNameIgnoreCase(
      UUID supplierId, String storeId, String name);
}
