package com.famora.finance.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public record FinanceSummaryResponse(
    YearMonth month,
    String currency,
    BigDecimal totalIncome,
    BigDecimal totalExpense,
    BigDecimal balance
) {
}
