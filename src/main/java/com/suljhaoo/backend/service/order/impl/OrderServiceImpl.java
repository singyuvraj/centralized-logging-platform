package com.suljhaoo.backend.service.order.impl;

import com.suljhaoo.backend.enity.auth.Store;
import com.suljhaoo.backend.enity.auth.User;
import com.suljhaoo.backend.enity.order.Order;
import com.suljhaoo.backend.enity.order.OrderItem;
import com.suljhaoo.backend.enity.stock.Supplier;
import com.suljhaoo.backend.model.request.order.CreateOrderRequest;
import com.suljhaoo.backend.model.request.order.OrderItemRequest;
import com.suljhaoo.backend.model.request.order.UpdateOrderRequest;
import com.suljhaoo.backend.model.response.order.OrderListResult;
import com.suljhaoo.backend.model.response.order.OrderResponse;
import com.suljhaoo.backend.repository.auth.StoreRepository;
import com.suljhaoo.backend.repository.auth.UserRepository;
import com.suljhaoo.backend.repository.order.OrderItemRepository;
import com.suljhaoo.backend.repository.order.OrderRepository;
import com.suljhaoo.backend.repository.stock.SupplierRepository;
import com.suljhaoo.backend.service.order.OrderService;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final UserRepository userRepository;
  private final StoreRepository storeRepository;
  private final SupplierRepository supplierRepository;

  @Override
  @Transactional
  public OrderResponse createOrder(String userId, CreateOrderRequest request) {
    // Validate user exists
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

    // Get user's first store (or validate storeId if provided in future)
    // For now, orders don't have storeId in request, but we need it from user's store
    // Actually, looking at Node.js, orders don't have storeId in request either
    // Let me check the mobile app - it has storeId in Order entity
    // For now, I'll get the first store for the user
    Store store =
        storeRepository.findByUser_IdAndIsDeletedFalseOrderByCreatedAtDesc(userId).stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("User has no store"));

    // Validate supplier exists
    Supplier supplier =
        supplierRepository
            .findById(UUID.fromString(request.getSupplierId()))
            .orElseThrow(() -> new RuntimeException("Supplier not found"));

    // Validate supplier belongs to store
    if (!supplier.getStoreId().equals(store.getId())) {
      throw new RuntimeException("Supplier does not belong to user's store");
    }

    // Validate items
    if (request.getItems() == null || request.getItems().isEmpty()) {
      throw new RuntimeException("Order must have at least one item");
    }

    // Validate totalItems matches items length
    if (request.getTotalItems() == null || request.getTotalItems() != request.getItems().size()) {
      throw new RuntimeException("Total items count must match the number of items");
    }

    // Validate each item
    for (OrderItemRequest item : request.getItems()) {
      if (item.getItemId() == null
          || item.getItemId().trim().isEmpty()
          || item.getItemName() == null
          || item.getItemName().trim().isEmpty()
          || item.getQuantity() == null
          || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0
          || item.getUnit() == null
          || item.getUnit().trim().isEmpty()) {
        throw new RuntimeException(
            "Each item must have itemId, itemName, quantity (greater than 0), and unit");
      }
    }

    // Validate status if provided
    String status = request.getStatus() != null ? request.getStatus().toLowerCase() : "ordered";
    if (!status.matches("^(ordered|received|cancelled|pending|closed)$")) {
      throw new RuntimeException(
          "Invalid order status. Must be one of: ordered, received, cancelled, pending, closed");
    }

    // Parse orderDate if provided
    LocalDateTime orderDate;
    if (request.getOrderDate() != null && !request.getOrderDate().trim().isEmpty()) {
      try {
        orderDate = LocalDateTime.parse(request.getOrderDate());
      } catch (DateTimeParseException e) {
        // Try parsing with timezone
        try {
          java.time.ZonedDateTime zonedDateTime =
              java.time.ZonedDateTime.parse(request.getOrderDate());
          orderDate = zonedDateTime.toLocalDateTime();
        } catch (DateTimeParseException ex) {
          throw new RuntimeException("Invalid order date format: " + request.getOrderDate());
        }
      }
    } else {
      orderDate = LocalDateTime.now();
    }

    // Create order
    Order order =
        Order.builder()
            .user(user)
            .store(store)
            .supplier(supplier)
            .supplierName(request.getSupplierName().trim())
            .supplierPhone(request.getSupplierPhone().trim())
            .totalItems(request.getTotalItems())
            .orderDate(orderDate)
            .status(status)
            .addedToStock(false)
            .isDirty(false)
            .items(new ArrayList<>())
            .build();

    order = orderRepository.saveAndFlush(order);

    // Create order items
    List<OrderItem> orderItems = new ArrayList<>();
    for (OrderItemRequest itemRequest : request.getItems()) {
      OrderItem orderItem =
          OrderItem.builder()
              .order(order)
              .storeId(store.getId())
              .itemId(itemRequest.getItemId().trim())
              .itemName(itemRequest.getItemName().trim())
              .quantity(itemRequest.getQuantity())
              .unit(itemRequest.getUnit().trim())
              .build();
      orderItems.add(orderItem);
    }
    orderItemRepository.saveAll(orderItems);
    order.getItems().addAll(orderItems);

    log.info(
        "Order created: {} for user: {}, supplier: {}",
        order.getId(),
        userId,
        request.getSupplierId());

    return mapToResponse(order);
  }

  @Override
  public OrderListResult getAllOrders(String userId, Integer limit, Integer skip) {
    // Validate user exists
    userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

    int pageSize = limit != null && limit > 0 ? limit : 100;
    int pageNumber = skip != null && skip >= 0 ? skip / pageSize : 0;
    Pageable pageable = PageRequest.of(pageNumber, pageSize);

    Page<Order> ordersPage = orderRepository.findByUser_IdOrderByOrderDateDesc(userId, pageable);

    List<OrderResponse> orders =
        ordersPage.getContent().stream().map(this::mapToResponse).collect(Collectors.toList());

    return OrderListResult.builder().orders(orders).total(ordersPage.getTotalElements()).build();
  }

  @Override
  public OrderListResult getOrdersBySupplier(String supplierId, Integer limit, Integer skip) {
    // Validate supplier exists
    Supplier supplier =
        supplierRepository
            .findById(UUID.fromString(supplierId))
            .orElseThrow(() -> new RuntimeException("Supplier not found"));

    int pageSize = limit != null && limit > 0 ? limit : 100;
    int pageNumber = skip != null && skip >= 0 ? skip / pageSize : 0;
    Pageable pageable = PageRequest.of(pageNumber, pageSize);

    Page<Order> ordersPage =
        orderRepository.findBySupplier_IdOrderByOrderDateDesc(supplier.getId(), pageable);

    List<OrderResponse> orders =
        ordersPage.getContent().stream().map(this::mapToResponse).collect(Collectors.toList());

    return OrderListResult.builder().orders(orders).total(ordersPage.getTotalElements()).build();
  }

  @Override
  public OrderResponse getOrderById(String orderId, String userId) {
    Order order =
        orderRepository
            .findByIdAndUser_Id(UUID.fromString(orderId), userId)
            .orElseThrow(() -> new RuntimeException("Order not found"));

    return mapToResponse(order);
  }

  @Override
  @Transactional
  public OrderResponse updateOrder(String orderId, String userId, UpdateOrderRequest request) {
    Order order =
        orderRepository
            .findByIdAndUser_Id(UUID.fromString(orderId), userId)
            .orElseThrow(() -> new RuntimeException("Order not found"));

    // Prevent updating received or closed orders
    if ("received".equals(order.getStatus()) || "closed".equals(order.getStatus())) {
      throw new RuntimeException("Cannot update an order with status 'received' or 'closed'");
    }

    // Update items if provided
    if (request.getItems() != null) {
      if (request.getItems().isEmpty()) {
        throw new RuntimeException("Order must have at least one item");
      }

      // Validate each item
      for (OrderItemRequest item : request.getItems()) {
        if (item.getItemId() == null
            || item.getItemId().trim().isEmpty()
            || item.getItemName() == null
            || item.getItemName().trim().isEmpty()
            || item.getQuantity() == null
            || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0
            || item.getUnit() == null
            || item.getUnit().trim().isEmpty()) {
          throw new RuntimeException(
              "Each item must have itemId, itemName, quantity (greater than 0), and unit");
        }
      }

      // Validate totalItems matches items length if both are provided
      if (request.getTotalItems() != null && request.getTotalItems() != request.getItems().size()) {
        throw new RuntimeException("Total items count must match the number of items");
      }

      // Delete existing order items
      orderItemRepository.deleteByOrder_Id(order.getId());

      // Create new order items
      List<OrderItem> orderItems = new ArrayList<>();
      for (OrderItemRequest itemRequest : request.getItems()) {
        OrderItem orderItem =
            OrderItem.builder()
                .order(order)
                .storeId(order.getStoreId())
                .itemId(itemRequest.getItemId().trim())
                .itemName(itemRequest.getItemName().trim())
                .quantity(itemRequest.getQuantity())
                .unit(itemRequest.getUnit().trim())
                .build();
        orderItems.add(orderItem);
      }
      orderItemRepository.saveAll(orderItems);
      order.setItems(orderItems);
    }

    // Update totalItems if provided
    if (request.getTotalItems() != null) {
      if (request.getTotalItems() <= 0) {
        throw new RuntimeException("Total items must be at least 1");
      }
      order.setTotalItems(request.getTotalItems());
    }

    // Parse and update orderDate if provided
    if (request.getOrderDate() != null && !request.getOrderDate().trim().isEmpty()) {
      try {
        LocalDateTime orderDate = LocalDateTime.parse(request.getOrderDate());
        order.setOrderDate(orderDate);
      } catch (DateTimeParseException e) {
        try {
          java.time.ZonedDateTime zonedDateTime =
              java.time.ZonedDateTime.parse(request.getOrderDate());
          order.setOrderDate(zonedDateTime.toLocalDateTime());
        } catch (DateTimeParseException ex) {
          throw new RuntimeException("Invalid order date format: " + request.getOrderDate());
        }
      }
    }

    // Update status if provided
    if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
      String status = request.getStatus().toLowerCase();
      if (!status.matches("^(ordered|received|cancelled|pending|closed)$")) {
        throw new RuntimeException(
            "Invalid order status. Must be one of: ordered, received, cancelled, pending, closed");
      }
      order.setStatus(status);
    }

    order = orderRepository.save(order);

    log.info("Order updated: {} for user: {}", orderId, userId);

    return mapToResponse(order);
  }

  @Override
  @Transactional
  public void deleteOrder(String orderId, String userId) {
    Order order =
        orderRepository
            .findByIdAndUser_Id(UUID.fromString(orderId), userId)
            .orElseThrow(() -> new RuntimeException("Order not found"));

    // Prevent deletion of received or closed orders
    if ("received".equals(order.getStatus()) || "closed".equals(order.getStatus())) {
      throw new RuntimeException("Cannot delete an order with status 'received' or 'closed'");
    }

    // Delete order items first (cascade should handle this, but being explicit)
    orderItemRepository.deleteByOrder_Id(order.getId());

    // Delete order
    orderRepository.delete(order);

    log.info("Order deleted: {} for user: {}", orderId, userId);
  }

  private OrderResponse mapToResponse(Order order) {
    // Fetch order items eagerly for response
    List<OrderItem> orderItems =
        orderItemRepository.findByOrder_IdOrderByCreatedAtAsc(order.getId());

    return OrderResponse.builder()
        .id(order.getId())
        .userId(order.getUserId())
        .storeId(order.getStoreId())
        .supplierId(order.getSupplierId() != null ? order.getSupplierId().toString() : null)
        .supplierName(order.getSupplierName())
        .supplierPhone(order.getSupplierPhone())
        .items(
            orderItems.stream()
                .map(
                    item ->
                        com.suljhaoo.backend.model.response.order.OrderItemResponse.builder()
                            .itemId(item.getItemId())
                            .itemName(item.getItemName())
                            .quantity(item.getQuantity())
                            .unit(item.getUnit())
                            .build())
                .collect(Collectors.toList()))
        .totalItems(order.getTotalItems())
        .orderDate(order.getOrderDate())
        .status(order.getStatus())
        .addedToStock(order.getAddedToStock())
        .createdAt(order.getCreatedAt())
        .updatedAt(order.getUpdatedAt())
        .build();
  }
}
