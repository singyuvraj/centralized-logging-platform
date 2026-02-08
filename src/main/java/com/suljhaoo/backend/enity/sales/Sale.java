package com.suljhaoo.backend.enity.sales;

import com.suljhaoo.backend.enity.auth.Store;
import com.suljhaoo.backend.enity.auth.User;
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

@Entity
@Table(
    name = "sales",
    indexes = {
      @Index(name = "idx_user_id", columnList = "user_id"),
      @Index(name = "idx_store_id", columnList = "store_id"),
      @Index(name = "idx_sale_date", columnList = "sale_date"),
      @Index(name = "idx_user_store_date", columnList = "user_id,store_id,sale_date"),
      @Index(name = "idx_store_date", columnList = "store_id,sale_date")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sale {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sale_user"))
  private User user;

  // Convenience getter for userId (for backward compatibility)
  public String getUserId() {
    return user != null ? user.getId() : null;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "store_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sale_store"))
  private Store store;

  // Convenience getter for storeId (for backward compatibility)
  public String getStoreId() {
    return store != null ? store.getId() : null;
  }

  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "payment_method", nullable = false, length = 20)
  private String paymentMethod; // 'cash', 'upi', 'card', 'credit'

  @Column(name = "customer_name", columnDefinition = "TEXT")
  private String customerName;

  @Column(name = "note", columnDefinition = "TEXT")
  private String note;

  @Column(name = "sale_date", nullable = false)
  private LocalDateTime saleDate; // Date when the sale actually occurred (can be in the past)

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "version", nullable = false)
  @Builder.Default
  private Integer version = 1;

  @Column(name = "updated_by", nullable = false, length = 20)
  @Builder.Default
  private String updatedBy = "web";
}
