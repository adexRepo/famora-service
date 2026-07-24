package com.famora.finance.entity;

import com.famora.common.entity.FamilyScopedEntity;
import com.famora.file.entity.FileAsset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "finance_debt_payments")
public class FinanceDebtPayment extends FamilyScopedEntity {
  
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "debt_id", nullable = false)
  private FinanceDebt debt;
  
  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;
  
  @Column(name = "payment_date", nullable = false)
  private LocalDate paymentDate;
  
  @Column(name = "notes", columnDefinition = "text")
  private String notes;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "attachment_file_id")
  private FileAsset attachmentFile;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "finance_transaction_id")
  private FinanceTransaction financeTransaction;
}
