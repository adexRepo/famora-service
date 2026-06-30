package com.famora.business.entity;

import com.famora.business.enums.PaymentMethod;
import com.famora.common.entity.BusinessScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "business_daily_payment_breakdowns")
public class BusinessDailyPaymentBreakdown extends BusinessScopedEntity {
  
  @Column(name = "daily_report_id", nullable = false)
  private UUID dailyReportId;
  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", nullable = false, length = 50)
  private PaymentMethod paymentMethod;
  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal amount;
  @Column(columnDefinition = "text")
  private String notes;
}
