package com.suljhaoo.backend.model.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
  private String name;

  @Email(message = "Invalid email format")
  private String email;

  private String address;

  @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
  private String phoneNumber;
}
