package com.famora.business.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BusinessSummaryResponse(UUID businessId, LocalDate fromDate, LocalDate toDate,
                                      BigDecimal totalSalesAmount, BigDecimal totalCashSalesAmount,
                                      BigDecimal totalQrisSalesAmount, BigDecimal totalTransferSalesAmount,
                                      BigDecimal totalOtherSalesAmount, BigDecimal totalExpenseAmount,
                                      BigDecimal totalCashExpenseAmount, BigDecimal totalNonCashExpenseAmount,
                                      BigDecimal totalLossAmount, BigDecimal netOperatingAmount,
                                      BigDecimal expectedCashAmount) {}
