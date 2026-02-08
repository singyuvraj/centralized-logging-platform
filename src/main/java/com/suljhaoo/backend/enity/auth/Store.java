package com.suljhaoo.backend.enity.auth;

import com.suljhaoo.backend.annotations.UlidGeneratorType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
    name = "stores",
    indexes = {
      @Index(name = "idx_user_id", columnList = "user_id"),
      @Index(name = "idx_user_deleted_active", columnList = "user_id,is_deleted,is_active")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Store {
  @Id @GeneratedValue @UlidGeneratorType private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_store_user"))
  private User user;

  // Convenience getter for userId (for backward compatibility)
  public String getUserId() {
    return user != null ? user.getId() : null;
  }

  @Column(name = "store_name", nullable = false)
  private String storeName;

  @Column(name = "store_address", columnDefinition = "TEXT")
  private String storeAddress;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  @Column(name = "is_deleted", nullable = false)
  @Builder.Default
  private Boolean isDeleted = false;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}
