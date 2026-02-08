package com.suljhaoo.backend.repository.auth;

import com.suljhaoo.backend.enity.auth.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
  Optional<User> findByPhoneNumber(String phoneNumber);

  @Query("SELECT u FROM User u WHERE u.phoneNumber = :phoneNumber")
  Optional<User> findByPhoneNumberWithPassword(@Param("phoneNumber") String phoneNumber);
}
