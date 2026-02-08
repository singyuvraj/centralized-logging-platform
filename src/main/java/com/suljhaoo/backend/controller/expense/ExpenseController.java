package com.suljhaoo.backend.controller.expense;

import com.suljhaoo.backend.aspect.ValidateUserAccess;
import com.suljhaoo.backend.model.request.expense.CreateExpenseRequest;
import com.suljhaoo.backend.model.request.expense.UpdateExpenseRequest;
import com.suljhaoo.backend.model.response.expense.ExpenseListResponse;
import com.suljhaoo.backend.model.response.expense.ExpenseResponse;
import com.suljhaoo.backend.model.response.expense.ExpenseSingleResponse;
import com.suljhaoo.backend.service.expense.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {

  private final ExpenseService expenseService;

  /** Create a new expense record POST /api/expenses/user/{userId}/{storeId} */
  @ValidateUserAccess
  @PostMapping("/user/{userId}/{storeId}")
  public ResponseEntity<ExpenseSingleResponse> createExpense(
      @PathVariable String userId,
      @PathVariable String storeId,
      @Valid @RequestBody CreateExpenseRequest request) {
    ExpenseResponse expense = expenseService.createExpense(userId, storeId, request);

    ExpenseSingleResponse response =
        ExpenseSingleResponse.builder()
            .status("success")
            .message("Expense created successfully")
            .data(ExpenseSingleResponse.ExpenseSingleData.builder().expense(expense).build())
            .build();

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /** Get all expenses for a specific user and store GET /api/expenses/user/{userId}/{storeId} */
  @ValidateUserAccess
  @GetMapping("/user/{userId}/{storeId}")
  public ResponseEntity<ExpenseListResponse> getAllExpenses(
      @PathVariable String userId,
      @PathVariable String storeId,
      @RequestParam(required = false) Integer limit,
      @RequestParam(required = false) Integer skip) {
    var result = expenseService.getAllExpenses(userId, storeId, limit, skip);

    ExpenseListResponse response =
        ExpenseListResponse.builder()
            .status("success")
            .message("Expenses retrieved successfully")
            .data(
                ExpenseListResponse.ExpenseListData.builder()
                    .expenses(result.getExpenses())
                    .total(result.getTotal())
                    .limit(limit != null ? limit : 1000)
                    .skip(skip != null ? skip : 0)
                    .build())
            .build();

    return ResponseEntity.ok(response);
  }

  /** Update an expense record PUT /api/expenses/user/{userId}/{storeId}/{expenseId} */
  @ValidateUserAccess
  @PutMapping("/user/{userId}/{storeId}/{expenseId}")
  public ResponseEntity<ExpenseSingleResponse> updateExpense(
      @PathVariable String userId,
      @PathVariable String storeId,
      @PathVariable String expenseId,
      @Valid @RequestBody UpdateExpenseRequest request) {
    ExpenseResponse expense = expenseService.updateExpense(expenseId, userId, storeId, request);

    ExpenseSingleResponse response =
        ExpenseSingleResponse.builder()
            .status("success")
            .message("Expense updated successfully")
            .data(ExpenseSingleResponse.ExpenseSingleData.builder().expense(expense).build())
            .build();

    return ResponseEntity.ok(response);
  }

  /** Delete an expense record DELETE /api/expenses/user/{userId}/{storeId}/{expenseId} */
  @ValidateUserAccess
  @DeleteMapping("/user/{userId}/{storeId}/{expenseId}")
  public ResponseEntity<ExpenseSingleResponse> deleteExpense(
      @PathVariable String userId, @PathVariable String storeId, @PathVariable String expenseId) {
    expenseService.deleteExpense(expenseId, userId, storeId);

    ExpenseSingleResponse response =
        ExpenseSingleResponse.builder()
            .status("success")
            .message("Expense deleted successfully")
            .build();

    return ResponseEntity.ok(response);
  }
}
