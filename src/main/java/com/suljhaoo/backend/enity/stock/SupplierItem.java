package com.suljhaoo.backend.enity.stock;

import com.suljhaoo.backend.enity.auth.Store;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * SupplierItem entity representing items available from a supplier. This entity can optionally link
 * to a stock item for inventory tracking.
 */
@Entity
@Table(
    name = "supplier_items",
    indexes = {
      @Index(name = "idx_supplier_item_supplier_id", columnList = "supplier_id"),
      @Index(name = "idx_supplier_item_store_id", columnList = "store_id"),
      @Index(name = "idx_supplier_item_stock_id", columnList = "stock_item_id"),
      @Index(name = "idx_supplier_item_name", columnList = "name"),
      @Index(name = "idx_supplier_item_store_supplier", columnList = "store_id,supplier_id")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierItem {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "supplier_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_supplier_item_supplier"))
  private Supplier supplier;

  // Convenience getter for supplierId (for backward compatibility)
  public UUID getSupplierId() {
    return supplier != null ? supplier.getId() : null;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "store_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_supplier_item_store"))
  private Store store;

  // Convenience getter for storeId (for backward compatibility)
  public String getStoreId() {
    return store != null ? store.getId() : null;
  }

  /**
   * Optional reference to stock item. When set, this supplier item is linked to an existing stock
   * item.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "stock_item_id", foreignKey = @ForeignKey(name = "fk_supplier_item_stock"))
  private Stock stock;

  // Convenience getter for stockItemId (for backward compatibility)
  public UUID getStockItemId() {
    return stock != null ? stock.getId() : null;
  }

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "unit", nullable = false, length = 50)
  private String unit;

  @Column(name = "category", length = 100)
  private String category;

  @Column(name = "current_stock", precision = 10, scale = 2)
  private BigDecimal currentStock;

  @Column(name = "min_level", precision = 10, scale = 2)
  private BigDecimal minLevel;

  @Column(name = "price", precision = 10, scale = 2)
  private BigDecimal price;

  // Sync fields for mobile app synchronization
  @Column(name = "server_id", length = 255)
  private String serverId;

  @Column(name = "is_dirty", nullable = false)
  @Builder.Default
  private Boolean isDirty = false;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  /**
   * Checks if the current stock is at or below the minimum level.
   *
   * @return true if currentStock <= minLevel, false otherwise
   */
  public boolean isLowStock() {
    if (currentStock == null || minLevel == null) {
      return false;
    }
    return currentStock.compareTo(minLevel) <= 0;
  }
}
