package com.suljhaoo.backend.repository.auth;

import com.suljhaoo.backend.enity.auth.Store;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreRepository extends JpaRepository<Store, String> {
  List<Store> findByUser_IdAndIsDeletedFalseOrderByCreatedAtDesc(String userId);

  Optional<Store> findByIdAndUser_IdAndIsDeletedFalse(String id, String userId);

  Optional<Store> findByIdAndUser_Id(String id, String userId);

  long countByUser_IdAndIsDeletedFalse(String userId);

  List<Store> findByUser_IdAndIsDeletedFalseAndIdNot(String userId, String excludeId);
}
