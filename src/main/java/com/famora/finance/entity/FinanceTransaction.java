package com.famora.finance.entity;

import com.famora.common.entity.FamilyScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
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
@Table(name = "finance_transactions")
public class FinanceTransaction extends FamilyScopedEntity {
  
  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private FinanceTransactionType type;
  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;
  @Column(name = "currency", nullable = false, length = 3)
  private String currency;
  @Column(name = "category", nullable = false, length = 100)
  private String category;
  @Column(name = "description", columnDefinition = "text")
  private String description;
  @Column(name = "transaction_date", nullable = false)
  private LocalDate transactionDate;
}
