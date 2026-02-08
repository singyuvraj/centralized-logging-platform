package com.suljhaoo.backend.controller.order;

import com.suljhaoo.backend.aspect.ValidateUserAccess;
import com.suljhaoo.backend.model.request.order.CreateOrderRequest;
import com.suljhaoo.backend.model.request.order.UpdateOrderRequest;
import com.suljhaoo.backend.model.request.order.UpdateOrderStatusRequest;
import com.suljhaoo.backend.model.response.order.OrderListResponse;
import com.suljhaoo.backend.model.response.order.OrderResponse;
import com.suljhaoo.backend.model.response.order.OrderSingleResponse;
import com.suljhaoo.backend.service.order.OrderService;
import com.suljhaoo.backend.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;

  /** Create a new order POST /api/orders */
  @PostMapping
  public ResponseEntity<OrderSingleResponse> createOrder(
      @Valid @RequestBody CreateOrderRequest request) {
    String userId = SecurityUtil.getCurrentUserId();
    OrderResponse order = orderService.createOrder(userId, request);

    OrderSingleResponse response =
        OrderSingleResponse.builder()
            .status("success")
            .message("Order created successfully")
            .data(OrderSingleResponse.OrderSingleData.builder().order(order).build())
            .build();

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /** Get all orders for the authenticated user GET /api/orders/user/{userId} */
  @ValidateUserAccess
  @GetMapping("/user/{userId}")
  public ResponseEntity<OrderListResponse> getAllOrders(
      @PathVariable String userId,
      @RequestParam(required = false) Integer limit,
      @RequestParam(required = false) Integer skip) {
    var result = orderService.getAllOrders(userId, limit, skip);

    OrderListResponse response =
        OrderListResponse.builder()
            .status("success")
            .data(
                OrderListResponse.OrderListData.builder()
                    .orders(result.getOrders())
                    .total(result.getTotal())
                    .limit(limit != null ? limit : 100)
                    .skip(skip != null ? skip : 0)
                    .build())
            .build();

    return ResponseEntity.ok(response);
  }

  /** Get all orders for a specific supplier GET /api/orders/supplier/{supplierId} */
  @GetMapping("/supplier/{supplierId}")
  public ResponseEntity<OrderListResponse> getOrdersBySupplier(
      @PathVariable String supplierId,
      @RequestParam(required = false) Integer limit,
      @RequestParam(required = false) Integer skip) {
    var result = orderService.getOrdersBySupplier(supplierId, limit, skip);

    OrderListResponse response =
        OrderListResponse.builder()
            .status("success")
            .data(
                OrderListResponse.OrderListData.builder()
                    .orders(result.getOrders())
                    .total(result.getTotal())
                    .limit(limit != null ? limit : 100)
                    .skip(skip != null ? skip : 0)
                    .build())
            .build();

    return ResponseEntity.ok(response);
  }

  /** Get a single order by ID GET /api/orders/user/{userId}/{id} */
  @ValidateUserAccess
  @GetMapping("/user/{userId}/{id}")
  public ResponseEntity<OrderSingleResponse> getOrderById(
      @PathVariable String userId, @PathVariable String id) {
    OrderResponse order = orderService.getOrderById(id, userId);

    OrderSingleResponse response =
        OrderSingleResponse.builder()
            .status("success")
            .data(OrderSingleResponse.OrderSingleData.builder().order(order).build())
            .build();

    return ResponseEntity.ok(response);
  }

  /** Update an order PUT /api/orders/user/{userId}/{id} */
  @ValidateUserAccess
  @PutMapping("/user/{userId}/{id}")
  public ResponseEntity<OrderSingleResponse> updateOrder(
      @PathVariable String userId,
      @PathVariable String id,
      @Valid @RequestBody UpdateOrderRequest request) {
    OrderResponse order = orderService.updateOrder(id, userId, request);

    OrderSingleResponse response =
        OrderSingleResponse.builder()
            .status("success")
            .message("Order updated successfully")
            .data(OrderSingleResponse.OrderSingleData.builder().order(order).build())
            .build();

    return ResponseEntity.ok(response);
  }

  /** Update order status PATCH /api/orders/user/{userId}/{id}/status */
  @ValidateUserAccess
  @PatchMapping("/user/{userId}/{id}/status")
  public ResponseEntity<OrderSingleResponse> updateOrderStatus(
      @PathVariable String userId,
      @PathVariable String id,
      @Valid @RequestBody UpdateOrderStatusRequest request) {
    UpdateOrderRequest updateRequest =
        UpdateOrderRequest.builder().status(request.getStatus()).build();
    OrderResponse order = orderService.updateOrder(id, userId, updateRequest);

    OrderSingleResponse response =
        OrderSingleResponse.builder()
            .status("success")
            .message("Order status updated successfully")
            .data(OrderSingleResponse.OrderSingleData.builder().order(order).build())
            .build();

    return ResponseEntity.ok(response);
  }

  /** Delete an order DELETE /api/orders/user/{userId}/{id} */
  @ValidateUserAccess
  @DeleteMapping("/user/{userId}/{id}")
  public ResponseEntity<OrderSingleResponse> deleteOrder(
      @PathVariable String userId, @PathVariable String id) {
    orderService.deleteOrder(id, userId);

    OrderSingleResponse response =
        OrderSingleResponse.builder()
            .status("success")
            .message("Order deleted successfully")
            .build();

    return ResponseEntity.ok(response);
  }
}
