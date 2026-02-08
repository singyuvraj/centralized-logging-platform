package com.suljhaoo.backend.service.expense.impl;

import com.suljhaoo.backend.enity.auth.Store;
import com.suljhaoo.backend.enity.auth.User;
import com.suljhaoo.backend.enity.expense.Expense;
import com.suljhaoo.backend.model.request.expense.CreateExpenseRequest;
import com.suljhaoo.backend.model.request.expense.UpdateExpenseRequest;
import com.suljhaoo.backend.model.response.expense.ExpenseListResult;
import com.suljhaoo.backend.model.response.expense.ExpenseResponse;
import com.suljhaoo.backend.repository.auth.StoreRepository;
import com.suljhaoo.backend.repository.auth.UserRepository;
import com.suljhaoo.backend.repository.expense.ExpenseRepository;
import com.suljhaoo.backend.service.expense.ExpenseService;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
public class ExpenseServiceImpl implements ExpenseService {

  private final ExpenseRepository expenseRepository;
  private final UserRepository userRepository;
  private final StoreRepository storeRepository;

  @Override
  @Transactional
  public ExpenseResponse createExpense(
      String userId, String storeId, CreateExpenseRequest request) {
    // Validate user exists
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

    // Validate store exists
    Store store =
        storeRepository
            .findById(storeId)
            .orElseThrow(() -> new RuntimeException("Store not found"));

    // Validate store belongs to user
    if (!store.getUser().getId().equals(userId)) {
      throw new RuntimeException("Store does not belong to user");
    }

    // Validate category
    if (request.getCategory() == null || request.getCategory().trim().isEmpty()) {
      throw new RuntimeException("Expense category is required");
    }

    // Validate amount
    if (request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
      throw new RuntimeException("Amount must be greater than 0");
    }

    // Validate payment method if provided
    String paymentMethod = null;
    if (request.getPaymentMethod() != null && !request.getPaymentMethod().trim().isEmpty()) {
      paymentMethod = request.getPaymentMethod().toLowerCase();
      if (!paymentMethod.matches("^(cash|upi|card|bank)$")) {
        throw new RuntimeException("Invalid payment method. Must be one of: cash, upi, card, bank");
      }
    } else {
      // Default payment method if not provided
      paymentMethod = "cash";
    }

    // Validate tag if provided
    String tag = null;
    if (request.getTag() != null && !request.getTag().trim().isEmpty()) {
      tag = request.getTag();
      if (!tag.matches("^(Store|Staff|Bank|Govt Fees|Mobility)$")) {
        throw new RuntimeException(
            "Invalid tag. Must be one of: Store, Staff, Bank, Govt Fees, Mobility");
      }
    } else {
      // Default tag if not provided
      tag = "Store";
    }

    // Parse and validate expenseDate if provided
    // The date comes from frontend as YYYY-MM-DD string (local date) or ISO datetime string
    // Parse it as local time to preserve the exact date selected by user
    LocalDateTime expenseDate;
    if (request.getExpenseDate() == null || request.getExpenseDate().trim().isEmpty()) {
      expenseDate = LocalDateTime.now();
    } else {
      String dateStr = request.getExpenseDate().trim();
      try {
        // Check if it's a date string (YYYY-MM-DD)
        if (dateStr.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
          // Parse YYYY-MM-DD as local date with noon time to avoid timezone shifting
          LocalDate localDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
          expenseDate = localDate.atTime(12, 0, 0); // Noon local time
        } else {
          // Try to parse as ISO datetime string
          try {
            expenseDate = LocalDateTime.parse(dateStr);
          } catch (DateTimeParseException e) {
            // If that fails, try parsing as ISO date-time with offset and extract just the date
            // This handles cases like "2026-01-21T10:00:00Z"
            java.time.ZonedDateTime zonedDateTime = java.time.ZonedDateTime.parse(dateStr);
            LocalDate localDate = zonedDateTime.toLocalDate();
            LocalDateTime now = LocalDateTime.now();
            // Create date with selected date and current local time (matching TypeScript behavior)
            expenseDate = localDate.atTime(now.toLocalTime());
          }
        }
      } catch (DateTimeParseException e) {
        throw new RuntimeException("Invalid expense date format: " + dateStr);
      }

      // Allow past dates but not future dates beyond reasonable limit (e.g., 1 day in future for
      // timezone issues)
      LocalDateTime maxFutureDate = LocalDateTime.now().plusDays(1);
      if (expenseDate.isAfter(maxFutureDate)) {
        throw new RuntimeException("Expense date cannot be in the future");
      }
    }

    // Create expense
    Expense expense =
        Expense.builder()
            .user(user)
            .store(store)
            .category(request.getCategory().trim())
            .amount(request.getAmount())
            .description(request.getDescription() != null ? request.getDescription().trim() : null)
            .expenseDate(expenseDate)
            .paymentMethod(paymentMethod)
            .tag(tag)
            .billImageUrl(null) // Will be set later if images are uploaded
            .isDirty(false)
            .build();

    expense = expenseRepository.saveAndFlush(expense);

    log.info("Expense created: {} by user: {} for store: {}", expense.getId(), userId, storeId);

    return mapToResponse(expense);
  }

  @Override
  public ExpenseListResult getAllExpenses(
      String userId, String storeId, Integer limit, Integer skip) {
    // Validate store exists and belongs to user
    Store store =
        storeRepository
            .findById(storeId)
            .orElseThrow(() -> new RuntimeException("Store not found"));

    if (!store.getUser().getId().equals(userId)) {
      throw new RuntimeException("Store does not belong to user");
    }

    int pageSize =
        limit != null && limit > 0 ? limit : 1000; // Default 1000 to match TypeScript backend
    int pageNumber = skip != null && skip >= 0 ? skip / pageSize : 0;
    Pageable pageable = PageRequest.of(pageNumber, pageSize);

    Page<Expense> expensesPage =
        expenseRepository.findByUser_IdAndStore_IdOrderByExpenseDateDesc(userId, storeId, pageable);

    List<ExpenseResponse> expenses =
        expensesPage.getContent().stream().map(this::mapToResponse).collect(Collectors.toList());

    return ExpenseListResult.builder()
        .expenses(expenses)
        .total(expensesPage.getTotalElements())
        .build();
  }

  @Override
  @Transactional
  public ExpenseResponse updateExpense(
      String expenseId, String userId, String storeId, UpdateExpenseRequest request) {
    // Find expense by ID, userId, and storeId
    Expense expense =
        expenseRepository
            .findByIdAndUser_IdAndStore_Id(UUID.fromString(expenseId), userId, storeId)
            .orElseThrow(() -> new RuntimeException("Expense not found"));

    // Update category if provided
    if (request.getCategory() != null) {
      String trimmedCategory = request.getCategory().trim();
      if (trimmedCategory.isEmpty()) {
        throw new RuntimeException("Expense category cannot be empty");
      }
      expense.setCategory(trimmedCategory);
    }

    // Validate and update amount if provided
    if (request.getAmount() != null) {
      if (request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
        throw new RuntimeException("Amount must be greater than 0");
      }
      expense.setAmount(request.getAmount());
    }

    // Update description if provided (allow null to clear)
    if (request.getDescription() != null) {
      expense.setDescription(
          request.getDescription().trim().isEmpty() ? null : request.getDescription().trim());
    }

    // Parse and validate expenseDate if provided
    if (request.getExpenseDate() != null && !request.getExpenseDate().trim().isEmpty()) {
      String dateStr = request.getExpenseDate().trim();
      LocalDateTime expenseDate;
      try {
        // Check if it's a date string (YYYY-MM-DD)
        if (dateStr.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
          // Parse YYYY-MM-DD as local date with noon time to avoid timezone shifting
          LocalDate localDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
          expenseDate = localDate.atTime(12, 0, 0); // Noon local time
        } else {
          // Try to parse as ISO datetime string
          try {
            expenseDate = LocalDateTime.parse(dateStr);
          } catch (DateTimeParseException e) {
            // If that fails, try parsing as ISO date-time with offset and extract just the date
            // This handles cases like "2026-01-21T10:00:00Z"
            java.time.ZonedDateTime zonedDateTime = java.time.ZonedDateTime.parse(dateStr);
            LocalDate localDate = zonedDateTime.toLocalDate();
            LocalDateTime now = LocalDateTime.now();
            // Create date with selected date and current local time (matching TypeScript behavior)
            expenseDate = localDate.atTime(now.toLocalTime());
          }
        }
      } catch (DateTimeParseException e) {
        throw new RuntimeException("Invalid expense date format: " + dateStr);
      }

      // Allow past dates but not future dates beyond reasonable limit (e.g., 1 day in future for
      // timezone issues)
      LocalDateTime maxFutureDate = LocalDateTime.now().plusDays(1);
      if (expenseDate.isAfter(maxFutureDate)) {
        throw new RuntimeException("Expense date cannot be in the future");
      }
      expense.setExpenseDate(expenseDate);
    }

    // Validate and update paymentMethod if provided
    if (request.getPaymentMethod() != null && !request.getPaymentMethod().trim().isEmpty()) {
      String paymentMethod = request.getPaymentMethod().toLowerCase();
      if (!paymentMethod.matches("^(cash|upi|card|bank)$")) {
        throw new RuntimeException("Invalid payment method. Must be one of: cash, upi, card, bank");
      }
      expense.setPaymentMethod(paymentMethod);
    }

    // Validate and update tag if provided
    if (request.getTag() != null && !request.getTag().trim().isEmpty()) {
      String tag = request.getTag();
      if (!tag.matches("^(Store|Staff|Bank|Govt Fees|Mobility)$")) {
        throw new RuntimeException(
            "Invalid tag. Must be one of: Store, Staff, Bank, Govt Fees, Mobility");
      }
      expense.setTag(tag);
    }

    expense = expenseRepository.save(expense);

    log.info("Expense updated: {} for user: {}", expenseId, userId);

    return mapToResponse(expense);
  }

  @Override
  @Transactional
  public void deleteExpense(String expenseId, String userId, String storeId) {
    Expense expense =
        expenseRepository
            .findByIdAndUser_IdAndStore_Id(UUID.fromString(expenseId), userId, storeId)
            .orElseThrow(() -> new RuntimeException("Expense not found"));

    expenseRepository.delete(expense);

    log.info("Expense deleted: {} for user: {}", expenseId, userId);
  }

  private ExpenseResponse mapToResponse(Expense expense) {
    return ExpenseResponse.builder()
        .id(expense.getId())
        .userId(expense.getUserId())
        .storeId(expense.getStoreId())
        .category(expense.getCategory())
        .amount(expense.getAmount())
        .description(expense.getDescription())
        .expenseDate(expense.getExpenseDate())
        .paymentMethod(expense.getPaymentMethod())
        .tag(expense.getTag())
        .billImageUrl(expense.getBillImageUrl())
        .createdAt(expense.getCreatedAt())
        .updatedAt(expense.getUpdatedAt())
        .build();
  }
}
