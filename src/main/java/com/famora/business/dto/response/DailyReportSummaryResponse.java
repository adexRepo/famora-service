package com.famora.business.dto.response;

import com.famora.business.enums.DailyReportStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DailyReportSummaryResponse(UUID id, UUID businessId, LocalDate reportDate, String shift, String currency,
                                         UUID reportedByUserId, BigDecimal dailyCapitalAmount,
                                         BigDecimal totalSalesAmount, BigDecimal totalCashSalesAmount,
                                         BigDecimal totalQrisSalesAmount, BigDecimal totalTransferSalesAmount,
                                         BigDecimal totalOtherSalesAmount, BigDecimal totalExpenseAmount,
                                         BigDecimal totalCashExpenseAmount, BigDecimal totalNonCashExpenseAmount,
                                         BigDecimal totalLossAmount, BigDecimal expectedCashAmount,
                                         BigDecimal netOperatingAmount, DailyReportStatus reportStatus, String notes,
                                         OffsetDateTime createdDt, OffsetDateTime updatedDt) {}
