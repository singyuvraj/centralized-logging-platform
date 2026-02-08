package com.suljhaoo.backend.service.expense;

import com.suljhaoo.backend.model.request.expense.CreateExpenseRequest;
import com.suljhaoo.backend.model.request.expense.UpdateExpenseRequest;
import com.suljhaoo.backend.model.response.expense.ExpenseListResult;
import com.suljhaoo.backend.model.response.expense.ExpenseResponse;

public interface ExpenseService {
  ExpenseResponse createExpense(String userId, String storeId, CreateExpenseRequest request);

  ExpenseListResult getAllExpenses(String userId, String storeId, Integer limit, Integer skip);

  ExpenseResponse updateExpense(
      String expenseId, String userId, String storeId, UpdateExpenseRequest request);

  void deleteExpense(String expenseId, String userId, String storeId);
}
