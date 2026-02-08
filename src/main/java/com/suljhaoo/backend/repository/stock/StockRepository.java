package com.suljhaoo.backend.repository.stock;

import com.suljhaoo.backend.enity.stock.Stock;
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
public interface StockRepository extends JpaRepository<Stock, UUID> {
  // Get all stocks for a store
  List<Stock> findByStore_IdOrderByNameAsc(String storeId);

  // Get all stocks for a store with pagination
  Page<Stock> findByStore_IdOrderByNameAsc(String storeId, Pageable pageable);

  // Get a single stock by ID and store ID
  Optional<Stock> findByIdAndStore_Id(UUID id, String storeId);

  // Get stocks by category for a store
  @Query(
      "SELECT s FROM Stock s WHERE s.store.id = :storeId AND s.category = :category ORDER BY s.name ASC")
  List<Stock> findByStore_IdAndCategory(
      @Param("storeId") String storeId, @Param("category") String category);

  // Get low stock items (quantity <= minLevel) for a store
  @Query(
      "SELECT s FROM Stock s WHERE s.store.id = :storeId AND s.quantity <= s.minLevel ORDER BY s.name ASC")
  List<Stock> findLowStockItems(@Param("storeId") String storeId);

  // Get low stock items (quantity <= minLevel) for a store with pagination
  // Sorted by quantity ASC (lowest first), then name ASC (matching Node.js backend)
  @Query(
      "SELECT s FROM Stock s WHERE s.store.id = :storeId AND s.quantity <= s.minLevel ORDER BY s.quantity ASC, s.name ASC")
  Page<Stock> findLowStockItems(@Param("storeId") String storeId, Pageable pageable);

  // Search stocks by name or category for a store
  @Query(
      "SELECT s FROM Stock s WHERE s.store.id = :storeId "
          + "AND (LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) "
          + "OR LOWER(s.category) LIKE LOWER(CONCAT('%', :query, '%'))) "
          + "ORDER BY s.name ASC")
  List<Stock> searchStocks(@Param("storeId") String storeId, @Param("query") String query);

  // Find stock by name and store ID (for bulk upload - update if exists)
  Optional<Stock> findByStore_IdAndName(String storeId, String name);
}
