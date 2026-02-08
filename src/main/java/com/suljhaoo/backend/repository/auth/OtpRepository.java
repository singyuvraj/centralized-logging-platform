package com.suljhaoo.backend.repository.auth;

import com.suljhaoo.backend.enity.auth.Otp;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpRepository extends JpaRepository<Otp, String> {
  Optional<Otp> findByPhoneNumber(String phoneNumber);

  @Modifying
  @Query("DELETE FROM Otp o WHERE o.phoneNumber = :phoneNumber")
  void deleteByPhoneNumber(@Param("phoneNumber") String phoneNumber);

  @Modifying
  @Query("DELETE FROM Otp o WHERE o.createdAt < :expiryTime")
  void deleteExpiredOtps(@Param("expiryTime") LocalDateTime expiryTime);
}
