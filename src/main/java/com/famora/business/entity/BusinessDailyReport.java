package com.famora.business.entity;

import com.famora.business.constant.BusinessDefaults;
import com.famora.business.enums.DailyReportStatus;
import com.famora.common.entity.BusinessScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "business_daily_reports")
public class BusinessDailyReport extends BusinessScopedEntity {
  
  @Column(name = "report_date", nullable = false)
  private LocalDate reportDate;
  @Column(nullable = false, length = 50)
  private String shift = BusinessDefaults.SHIFT;
  @Column(nullable = false, length = 10)
  private String currency = BusinessDefaults.CURRENCY;
  @Column(name = "reported_by_user_id", nullable = false)
  private UUID reportedByUserId;
  @Column(name = "daily_capital_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal dailyCapitalAmount = BigDecimal.ZERO;
  @Column(name = "daily_capital_note", columnDefinition = "text")
  private String dailyCapitalNote;
  @Column(name = "total_sales_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalSalesAmount = BigDecimal.ZERO;
  @Column(name = "total_cash_sales_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalCashSalesAmount = BigDecimal.ZERO;
  @Column(name = "total_qris_sales_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalQrisSalesAmount = BigDecimal.ZERO;
  @Column(name = "total_transfer_sales_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalTransferSalesAmount = BigDecimal.ZERO;
  @Column(name = "total_other_sales_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalOtherSalesAmount = BigDecimal.ZERO;
  @Column(name = "total_expense_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalExpenseAmount = BigDecimal.ZERO;
  @Column(name = "total_cash_expense_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalCashExpenseAmount = BigDecimal.ZERO;
  @Column(name = "total_non_cash_expense_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalNonCashExpenseAmount = BigDecimal.ZERO;
  @Column(name = "total_loss_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalLossAmount = BigDecimal.ZERO;
  @Column(name = "expected_cash_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal expectedCashAmount = BigDecimal.ZERO;
  @Column(name = "net_operating_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal netOperatingAmount = BigDecimal.ZERO;
  @Enumerated(EnumType.STRING)
  @Column(name = "report_status", nullable = false, length = 30)
  private DailyReportStatus reportStatus = DailyReportStatus.DRAFT;
  @Column(columnDefinition = "text")
  private String notes;
  @Column(name = "approved_by_user_id")
  private UUID approvedByUserId;
  @Column(name = "approved_at")
  private OffsetDateTime approvedAt;
  @Column(name = "rejected_by_user_id")
  private UUID rejectedByUserId;
  @Column(name = "rejected_at")
  private OffsetDateTime rejectedAt;
  @Column(name = "rejection_reason", columnDefinition = "text")
  private String rejectionReason;
  
  @Column(name = "submitted_at")
  private OffsetDateTime submittedAt;
  
  @Column(name = "revision_requested_at")
  private OffsetDateTime revisionRequestedAt;
  
  @Column(name = "revision_requested_by_user_id")
  private UUID revisionRequestedByUserId;
  
  @Column(name = "revision_reason")
  private String revisionReason;
  
  @Column(name = "voided_by_user_id")
  private UUID voidedByUserId;
  
  @Column(name = "voided_at")
  private OffsetDateTime voidedAt;
  
  @Column(name = "void_reason")
  private String voidReason;
  
  @Version
  @Column(name = "version", nullable = false)
  private Long version;
  
}
