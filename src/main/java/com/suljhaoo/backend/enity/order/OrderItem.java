package com.suljhaoo.backend.enity.order;

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
 * OrderItem entity representing individual items in an order. This is a separate entity to support
 * the one-to-many relationship with Order (matching mobile app schema).
 */
@Entity
@Table(
    name = "order_items",
    indexes = {
      // Index for order items queries: findByOrder_IdOrderByCreatedAtAsc, deleteByOrder_Id
      @Index(name = "idx_order_item_order_id", columnList = "order_id")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "order_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_order_item_order"))
  private Order order;

  // Convenience getter for orderId (for backward compatibility)
  public UUID getOrderId() {
    return order != null ? order.getId() : null;
  }

  @Column(name = "store_id", nullable = false, length = 26)
  private String storeId;

  @Column(name = "item_id", nullable = false, length = 255)
  private String itemId;

  @Column(name = "item_name", nullable = false, length = 255)
  private String itemName;

  @Column(name = "quantity", nullable = false, precision = 10, scale = 2)
  private BigDecimal quantity;

  @Column(name = "unit", nullable = false, length = 50)
  private String unit;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}
