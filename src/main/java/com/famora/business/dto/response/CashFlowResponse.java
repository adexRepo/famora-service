package com.famora.business.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CashFlowResponse(UUID businessId, LocalDate fromDate, LocalDate toDate,
                               BigDecimal cashIn, BigDecimal cashOut, BigDecimal netCashFlow,
                               BigDecimal salesAmount, BigDecimal expenseAmount) {}
