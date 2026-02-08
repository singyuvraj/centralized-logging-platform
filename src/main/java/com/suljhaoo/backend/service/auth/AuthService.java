package com.suljhaoo.backend.service.auth;

import com.suljhaoo.backend.model.request.auth.LoginRequest;
import com.suljhaoo.backend.model.response.auth.LoginData;

public interface AuthService {
  LoginData login(LoginRequest request);
}
