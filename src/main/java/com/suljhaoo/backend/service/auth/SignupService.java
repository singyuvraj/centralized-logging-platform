package com.suljhaoo.backend.service.auth;

import com.suljhaoo.backend.enity.auth.User;

public interface SignupService {
  boolean checkPhoneNumberExists(String phoneNumber);

  User createUser(String name, String phoneNumber, String password);
}
