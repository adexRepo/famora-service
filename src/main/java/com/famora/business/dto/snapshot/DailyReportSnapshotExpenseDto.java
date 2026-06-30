package com.famora.business.dto.snapshot;

import com.famora.business.entity.BusinessExpense;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DailyReportSnapshotExpenseDto(
    UUID id,
    LocalDate expenseDate,
    String expenseName,
    String category,
    BigDecimal quantity,
    String unit,
    BigDecimal amount,
    String paymentMethod,
    String status,
    String notes
) {

  public static DailyReportSnapshotExpenseDto from(BusinessExpense expense) {
    return new DailyReportSnapshotExpenseDto(
        expense.getId(),
        expense.getExpenseDate(),
        expense.getExpenseName(),
        String.valueOf(expense.getCategory()),
        expense.getQuantity(),
        expense.getUnit() == null ? null : expense.getUnit(),
        expense.getAmount(),
        String.valueOf(expense.getPaymentMethod()),
        String.valueOf(expense.getStatus()),
        expense.getNotes()
    );
  }
}
