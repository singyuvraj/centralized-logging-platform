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
    name = "users",
    indexes = {@Index(name = "idx_phone_number", columnList = "phone_number", unique = true)})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
  @Id @GeneratedValue @UlidGeneratorType private String id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "phone_number", nullable = false, unique = true)
  private String phoneNumber;

  @Column(name = "email")
  private String email;

  @Column(name = "address", columnDefinition = "TEXT")
  private String address;

  @Column(name = "password", nullable = false)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private UserRole role;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  @Column(name = "last_login")
  private LocalDateTime lastLogin;

  @Column(name = "login_attempts")
  @Builder.Default
  private Integer loginAttempts = 0;

  @Column(name = "account_locked")
  @Builder.Default
  private Boolean accountLocked = false;

  @Column(name = "lock_until")
  private LocalDateTime lockUntil;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}
