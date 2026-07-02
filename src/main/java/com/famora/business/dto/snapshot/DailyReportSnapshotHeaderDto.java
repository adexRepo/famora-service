package com.famora.business.dto.snapshot;

import com.famora.business.entity.BusinessDailyReport;
import com.famora.business.enums.DailyReportStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DailyReportSnapshotHeaderDto(
    UUID id,
    UUID businessId,
    LocalDate reportDate,
    String shift,
    String currency,
    UUID reportedByUserId,
    BigDecimal dailyCapitalAmount,
    String dailyCapitalNote,
    BigDecimal totalSalesAmount,
    BigDecimal totalCashSalesAmount,
    BigDecimal totalQrisSalesAmount,
    BigDecimal totalTransferSalesAmount,
    BigDecimal totalOtherSalesAmount,
    BigDecimal totalExpenseAmount,
    BigDecimal totalCashExpenseAmount,
    BigDecimal totalNonCashExpenseAmount,
    BigDecimal totalLossAmount,
    BigDecimal expectedCashAmount,
    BigDecimal netOperatingAmount,
    DailyReportStatus status,
    String notes,
    OffsetDateTime submittedAt,
    OffsetDateTime revisionRequestedAt,
    UUID revisionRequestedByUserId,
    String revisionReason,
    UUID approvedByUserId,
    OffsetDateTime approvedAt,
    UUID rejectedByUserId,
    OffsetDateTime rejectedAt,
    String rejectionReason,
    UUID voidedByUserId,
    OffsetDateTime voidedAt,
    String voidReason,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {

  public static DailyReportSnapshotHeaderDto from(BusinessDailyReport report) {
    return new DailyReportSnapshotHeaderDto(
        report.getId(),
        report.getBusiness().getId(),
        report.getReportDate(),
        report.getShift(),
        report.getCurrency(),
        report.getReportedByUserId(),
        report.getDailyCapitalAmount(),
        report.getDailyCapitalNote(),
        report.getTotalSalesAmount(),
        report.getTotalCashSalesAmount(),
        report.getTotalQrisSalesAmount(),
        report.getTotalTransferSalesAmount(),
        report.getTotalOtherSalesAmount(),
        report.getTotalExpenseAmount(),
        report.getTotalCashExpenseAmount(),
        report.getTotalNonCashExpenseAmount(),
        report.getTotalLossAmount(),
        report.getExpectedCashAmount(),
        report.getNetOperatingAmount(),
        report.getReportStatus(),
        report.getNotes(),
        report.getSubmittedAt(),
        report.getRevisionRequestedAt(),
        report.getRevisionRequestedByUserId(),
        report.getRevisionReason(),
        report.getApprovedByUserId(),
        report.getApprovedAt(),
        report.getRejectedByUserId(),
        report.getRejectedAt(),
        report.getRejectionReason(),
        report.getVoidedByUserId(),
        report.getVoidedAt(),
        report.getVoidReason(),
        report.getCreatedAt(),
        report.getUpdatedAt()
    );
  }
}
