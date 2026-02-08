package com.suljhaoo.backend.service.order;

import com.suljhaoo.backend.model.request.order.CreateOrderRequest;
import com.suljhaoo.backend.model.request.order.UpdateOrderRequest;
import com.suljhaoo.backend.model.response.order.OrderListResult;
import com.suljhaoo.backend.model.response.order.OrderResponse;

public interface OrderService {
  OrderResponse createOrder(String userId, CreateOrderRequest request);

  OrderListResult getAllOrders(String userId, Integer limit, Integer skip);

  OrderListResult getOrdersBySupplier(String supplierId, Integer limit, Integer skip);

  OrderResponse getOrderById(String orderId, String userId);

  OrderResponse updateOrder(String orderId, String userId, UpdateOrderRequest request);

  void deleteOrder(String orderId, String userId);
}
