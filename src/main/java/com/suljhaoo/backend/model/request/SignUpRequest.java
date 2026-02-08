package com.suljhaoo.backend.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SignUpRequest {

  @NotBlank(message = "name should not be blank")
  private String name;

  @NotBlank(message = "Phone number is required")
  @Pattern(regexp = "^\\d{10}$", message = "Phone number should be valid")
  private String phoneNumber;
}
