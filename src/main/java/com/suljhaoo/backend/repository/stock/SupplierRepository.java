package com.suljhaoo.backend.repository.stock;

import com.suljhaoo.backend.enity.stock.Supplier;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
  // Get all suppliers for a store with pagination
  Page<Supplier> findByStore_IdOrderByCreatedAtDesc(String storeId, Pageable pageable);

  // Get all suppliers for a store (no pagination)
  List<Supplier> findByStore_IdOrderByNameAsc(String storeId);

  // Get a single supplier by ID and store ID
  Optional<Supplier> findByIdAndStore_Id(UUID id, String storeId);

  // Get a single supplier by ID, user ID, and store ID (for validation)
  Optional<Supplier> findByIdAndStore_User_IdAndStore_Id(UUID id, String userId, String storeId);

  // Count suppliers for a store
  long countByStore_Id(String storeId);
}
