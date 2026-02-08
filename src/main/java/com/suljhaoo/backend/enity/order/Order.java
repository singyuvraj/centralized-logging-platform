package com.suljhaoo.backend.enity.order;

import com.suljhaoo.backend.enity.auth.Store;
import com.suljhaoo.backend.enity.auth.User;
import com.suljhaoo.backend.enity.stock.Supplier;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Order entity representing orders placed to suppliers. This entity tracks order information and
 * status. Order items are stored in a separate OrderItem table (one-to-many relationship).
 */
@Entity
@Table(
    name = "orders",
    indexes = {
      // Composite index for user queries with date sorting: findByUser_IdOrderByOrderDateDesc
      @Index(name = "idx_order_user_date", columnList = "user_id,order_date"),
      // Composite index for supplier queries with date sorting:
      // findBySupplier_IdOrderByOrderDateDesc
      @Index(name = "idx_order_supplier_date", columnList = "supplier_id,order_date")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_user"))
  private User user;

  // Convenience getter for userId (for backward compatibility)
  public String getUserId() {
    return user != null ? user.getId() : null;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "store_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_order_store"))
  private Store store;

  // Convenience getter for storeId (for backward compatibility)
  public String getStoreId() {
    return store != null ? store.getId() : null;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "supplier_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_order_supplier"))
  private Supplier supplier;

  // Convenience getter for supplierId (for backward compatibility)
  public UUID getSupplierId() {
    return supplier != null ? supplier.getId() : null;
  }

  @Column(name = "supplier_name", nullable = false, length = 255)
  private String supplierName;

  @Column(name = "supplier_phone", nullable = false, length = 20)
  private String supplierPhone;

  @OneToMany(
      mappedBy = "order",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private List<OrderItem> items;

  @Column(name = "total_items", nullable = false)
  private Integer totalItems;

  @Column(name = "order_date", nullable = false)
  private LocalDateTime orderDate;

  @Column(name = "status", nullable = false, length = 20)
  private String status; // 'ordered', 'received', 'cancelled', 'pending', 'closed'

  @Column(name = "added_to_stock")
  private Boolean addedToStock;

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
}
