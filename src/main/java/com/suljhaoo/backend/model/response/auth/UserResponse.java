package com.suljhaoo.backend.model.response.auth;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
  private String id;
  private String name;
  private String phoneNumber;
  private String email;
  private String role;
  private Boolean isActive;
  private LocalDateTime lastLogin;
  private LocalDateTime createdAt;
}
