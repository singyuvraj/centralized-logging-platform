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
 * Stock entity representing inventory items in the store. This entity tracks stock quantities,
 * minimum levels, pricing, and supplier information.
 */
@Entity
@Table(
    name = "stocks",
    indexes = {
      @Index(name = "idx_stock_store_id", columnList = "store_id"),
      @Index(name = "idx_stock_supplier_id", columnList = "supplier_id"),
      @Index(name = "idx_stock_category", columnList = "category"),
      @Index(name = "idx_stock_name", columnList = "name"),
      @Index(name = "idx_stock_store_category", columnList = "store_id,category"),
      @Index(name = "idx_stock_low_stock", columnList = "store_id,quantity,min_level")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stock {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "quantity", nullable = false, precision = 10, scale = 2)
  private BigDecimal quantity;

  @Column(name = "min_level", nullable = false, precision = 10, scale = 2)
  private BigDecimal minLevel;

  @Column(name = "unit", length = 50)
  private String unit;

  @Column(name = "category", length = 100)
  private String category;

  @Column(name = "unit_price", precision = 10, scale = 2)
  private BigDecimal unitPrice;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "supplier_id", foreignKey = @ForeignKey(name = "fk_stock_supplier"))
  private Supplier supplier;

  // Convenience getter for supplierId (for backward compatibility)
  public UUID getSupplierId() {
    return supplier != null ? supplier.getId() : null;
  }

  @Column(name = "supplier_name", length = 255)
  private String supplierName;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "store_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_stock_store"))
  private Store store;

  // Convenience getter for storeId (for backward compatibility)
  public String getStoreId() {
    return store != null ? store.getId() : null;
  }

  // Sync fields for mobile app synchronization
  @Column(name = "server_id", length = 255)
  private String serverId;

  @Column(name = "last_synced_at")
  private LocalDateTime lastSyncedAt;

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
   * Checks if the stock quantity is at or below the minimum level.
   *
   * @return true if quantity <= minLevel, false otherwise
   */
  public boolean isLowStock() {
    return quantity != null && minLevel != null && quantity.compareTo(minLevel) <= 0;
  }

  /**
   * Calculates the total value of the stock (quantity * unitPrice).
   *
   * @return total value as BigDecimal, or null if unitPrice is null
   */
  public BigDecimal getTotalValue() {
    if (unitPrice == null || quantity == null) {
      return null;
    }
    return quantity.multiply(unitPrice);
  }
}
