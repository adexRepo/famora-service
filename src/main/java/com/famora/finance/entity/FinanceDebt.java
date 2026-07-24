package com.famora.finance.entity;

import com.famora.common.entity.FamilyScopedEntity;
import com.famora.file.entity.FileAsset;
import com.famora.finance.helper.FinanceDebtStatus;
import com.famora.finance.helper.FinanceDebtType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "finance_debts")
public class FinanceDebt extends FamilyScopedEntity {
  
  @Enumerated(EnumType.STRING)
  @Column(name = "debt_type", nullable = false, length = 30)
  private FinanceDebtType debtType;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "debt_status", nullable = false, length = 30)
  private FinanceDebtStatus debtStatus;
  
  @Column(name = "counterparty_name", nullable = false, length = 180)
  private String counterpartyName;
  
  @Column(name = "principal_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal principalAmount;
  
  @Column(name = "paid_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal paidAmount;
  
  @Column(name = "remaining_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal remainingAmount;
  
  @Column(name = "currency", nullable = false, length = 3)
  private String currency;
  
  @Column(name = "borrowed_date", nullable = false)
  private LocalDate borrowedDate;
  
  @Column(name = "due_date")
  private LocalDate dueDate;
  
  @Column(name = "notes", columnDefinition = "text")
  private String notes;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "attachment_file_id")
  private FileAsset attachmentFile;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "principal_finance_transaction_id")
  private FinanceTransaction principalFinanceTransaction;
}
