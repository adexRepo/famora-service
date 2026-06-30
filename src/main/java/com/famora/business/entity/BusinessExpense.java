package com.famora.business.entity;

import com.famora.business.enums.ExpenseCategory;
import com.famora.business.enums.PaymentMethod;
import com.famora.common.entity.BusinessScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "business_expenses")
public class BusinessExpense extends BusinessScopedEntity {
  
  @Column(name = "daily_report_id")
  private UUID dailyReportId;
  @Column(name = "expense_date", nullable = false)
  private LocalDate expenseDate;
  @Column(name = "expense_name", nullable = false, length = 150)
  private String expenseName;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 80)
  private ExpenseCategory category;
  @Column(precision = 18, scale = 2)
  private BigDecimal quantity;
  @Column(length = 50)
  private String unit;
  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal amount;
  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", nullable = false, length = 50)
  private PaymentMethod paymentMethod = PaymentMethod.CASH;
  @Column(columnDefinition = "text")
  private String notes;
  
}
