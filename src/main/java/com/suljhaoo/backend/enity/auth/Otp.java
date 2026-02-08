package com.suljhaoo.backend.enity.auth;

import com.suljhaoo.backend.annotations.UlidGeneratorType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "otp",
    indexes = {@Index(name = "idx_otp_phone_number", columnList = "phone_number")})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Otp {
  @Id @GeneratedValue @UlidGeneratorType private String id;

  @Column(name = "phone_number", nullable = false)
  private String phoneNumber;

  @Column(name = "otp", nullable = false, length = 6)
  private String otp;

  @Column(name = "name", nullable = false)
  private String name;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  // Note: TTL (Time To Live) will be handled at application level
  // We'll check expiry in service layer (1 minute = 60 seconds)
}
