package com.famora.business.dto.response;

import com.famora.business.enums.ExpenseCategory;
import com.famora.business.enums.PaymentMethod;
import com.famora.common.helper.Status;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ExpenseResponse(UUID id, UUID businessId, UUID dailyReportId, LocalDate expenseDate,
                              String expenseName,
                              ExpenseCategory category, BigDecimal quantity, String unit,
                              BigDecimal amount,
                              PaymentMethod paymentMethod, String notes, Status status,
                              UUID createdByUserId, OffsetDateTime createdAt,
                              OffsetDateTime updatedAt) {
  
}
