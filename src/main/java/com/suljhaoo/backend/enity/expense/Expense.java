package com.suljhaoo.backend.enity.expense;

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
    name = "expenses",
    indexes = {
      @Index(name = "idx_expense_user_id", columnList = "user_id"),
      @Index(name = "idx_expense_store_id", columnList = "store_id"),
      @Index(name = "idx_expense_date", columnList = "expense_date"),
      @Index(name = "idx_expense_user_store_date", columnList = "user_id,store_id,expense_date"),
      @Index(name = "idx_expense_store_date", columnList = "store_id,expense_date"),
      @Index(name = "idx_expense_category", columnList = "category"),
      @Index(name = "idx_expense_tag", columnList = "tag"),
      @Index(name = "idx_expense_payment_method", columnList = "payment_method")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expense {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "user_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_expense_user"))
  private User user;

  // Convenience getter for userId (for backward compatibility)
  public String getUserId() {
    return user != null ? user.getId() : null;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "store_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_expense_store"))
  private Store store;

  // Convenience getter for storeId (for backward compatibility)
  public String getStoreId() {
    return store != null ? store.getId() : null;
  }

  @Column(name = "category", nullable = false, length = 255)
  private String category;

  @Column(name = "amount", nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "expense_date", nullable = false)
  private LocalDateTime expenseDate;

  @Column(name = "payment_method", nullable = false, length = 20)
  private String paymentMethod; // 'cash', 'upi', 'card', 'bank'

  @Column(name = "tag", nullable = false, length = 50)
  private String tag; // 'Store', 'Staff', 'Bank', 'Govt Fees', 'Mobility'

  @Column(name = "bill_image_url", columnDefinition = "TEXT")
  private String billImageUrl;

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
