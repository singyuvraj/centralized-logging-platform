package com.suljhaoo.backend.controller.auth;

import com.suljhaoo.backend.model.request.auth.LoginRequest;
import com.suljhaoo.backend.model.response.auth.LoginData;
import com.suljhaoo.backend.model.response.auth.LoginResponse;
import com.suljhaoo.backend.model.response.auth.LogoutResponse;
import com.suljhaoo.backend.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    LoginData loginData = authService.login(request);

    LoginResponse response =
        LoginResponse.builder()
            .status("success")
            .message("Login successful")
            .data(loginData)
            .build();

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @PostMapping("/logout")
  public ResponseEntity<LogoutResponse> logout() {
    log.info("User logged out");

    LogoutResponse response =
        LogoutResponse.builder().status("success").message("Logout successful").build();

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }
}
