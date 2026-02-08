package com.suljhaoo.backend.enity.stock;

import com.suljhaoo.backend.enity.auth.Store;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Supplier entity representing suppliers/vendors that provide items to the store. This entity
 * tracks supplier contact information and business details.
 */
@Entity
@Table(
    name = "suppliers",
    indexes = {
      @Index(name = "idx_supplier_store_id", columnList = "store_id"),
      @Index(name = "idx_supplier_name", columnList = "name"),
      @Index(name = "idx_supplier_phone", columnList = "phone"),
      @Index(name = "idx_supplier_store_name", columnList = "store_id,name")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Supplier {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "supplier_type", nullable = false, length = 20)
  @Builder.Default
  private String supplierType = "distributor"; // 'mahajan' or 'distributor'

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "nick_name", nullable = false, length = 255)
  private String nickName;

  @Column(name = "phone", nullable = false, length = 20)
  private String phone;

  @Column(name = "gstin", length = 255)
  private String gstin;

  @Column(name = "address", columnDefinition = "TEXT")
  private String address;

  @Column(name = "email", length = 255)
  private String email;

  @Column(name = "brands", columnDefinition = "TEXT")
  private String brands; // Comma-separated brand names

  @Column(name = "salesman", length = 255)
  private String salesman;

  @Column(name = "sales_person_phone", length = 20)
  private String salesPersonPhone;

  @Column(name = "distributor_name", length = 255)
  private String distributorName;

  @Column(name = "max_no_of_credit_bills")
  private Integer maxNoOfCreditBills;

  @Column(name = "max_credit_period", nullable = false, length = 50)
  @Builder.Default
  private String maxCreditPeriod = "EOM";

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "store_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_supplier_store"))
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
}
