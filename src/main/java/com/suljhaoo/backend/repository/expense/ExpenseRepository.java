package com.suljhaoo.backend.repository.expense;

import com.suljhaoo.backend.enity.expense.Expense;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
  // Get all expenses for a user and store with pagination, ordered by expense date descending
  Page<Expense> findByUser_IdAndStore_IdOrderByExpenseDateDesc(
      String userId, String storeId, Pageable pageable);

  // Get a single expense by ID, user ID, and store ID
  Optional<Expense> findByIdAndUser_IdAndStore_Id(UUID id, String userId, String storeId);
}
