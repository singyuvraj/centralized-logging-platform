package com.suljhaoo.backend.repository.order;

import com.suljhaoo.backend.enity.order.OrderItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
  // Get all order items for an order
  List<OrderItem> findByOrder_IdOrderByCreatedAtAsc(UUID orderId);

  // Delete all order items for an order (used when deleting order)
  void deleteByOrder_Id(UUID orderId);
}
