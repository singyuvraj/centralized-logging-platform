package com.suljhaoo.backend.repository.order;

import com.suljhaoo.backend.enity.order.Order;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
  // Get all orders for a user with pagination
  Page<Order> findByUser_IdOrderByOrderDateDesc(String userId, Pageable pageable);

  // Get all orders for a supplier with pagination
  Page<Order> findBySupplier_IdOrderByOrderDateDesc(UUID supplierId, Pageable pageable);

  // Get a single order by ID and user ID
  Optional<Order> findByIdAndUser_Id(UUID id, String userId);

  // Count orders for a user
  long countByUser_Id(String userId);

  // Count orders for a supplier
  long countBySupplier_Id(UUID supplierId);
}
