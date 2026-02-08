package com.suljhaoo.backend.repository.sales;

import com.suljhaoo.backend.enity.sales.Sale;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SaleRepository extends JpaRepository<Sale, UUID> {
  // Get all sales for a user and store with pagination
  Page<Sale> findByUser_IdAndStore_IdOrderBySaleDateDesc(
      String userId, String storeId, Pageable pageable);

  // Get all sales for a user and store (without pagination)
  List<Sale> findByUser_IdAndStore_IdOrderBySaleDateDesc(String userId, String storeId);

  // Get a single sale by ID, user ID, and store ID
  Optional<Sale> findByIdAndUser_IdAndStore_Id(UUID id, String userId, String storeId);

  // Get all cash sales (cash, upi, card) for a user and store with pagination
  @Query(
      "SELECT s FROM Sale s WHERE s.user.id = :userId AND s.store.id = :storeId "
          + "AND s.paymentMethod IN ('cash', 'upi', 'card') "
          + "ORDER BY s.saleDate DESC")
  Page<Sale> findCashSalesByUserAndStore(
      @Param("userId") String userId, @Param("storeId") String storeId, Pageable pageable);

  // Get all cash sales (cash, upi, card) for a user and store (without pagination)
  @Query(
      "SELECT s FROM Sale s WHERE s.user.id = :userId AND s.store.id = :storeId "
          + "AND s.paymentMethod IN ('cash', 'upi', 'card') "
          + "ORDER BY s.saleDate DESC")
  List<Sale> findCashSalesByUserAndStoreList(
      @Param("userId") String userId, @Param("storeId") String storeId);

  // Count cash sales for a user and store
  @Query(
      "SELECT COUNT(s) FROM Sale s WHERE s.user.id = :userId AND s.store.id = :storeId "
          + "AND s.paymentMethod IN ('cash', 'upi', 'card')")
  long countCashSalesByUserAndStore(
      @Param("userId") String userId, @Param("storeId") String storeId);
}
