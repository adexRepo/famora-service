package com.famora.business.dto.response;

import com.famora.business.enums.ExpenseCategory;
import java.math.BigDecimal;

public record ExpenseCategorySummaryResponse(ExpenseCategory category, BigDecimal totalAmount) {}
