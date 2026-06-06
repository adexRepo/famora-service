package com.famora.finance.entity;

import com.famora.common.entity.BaseTimeEntity;
import com.famora.family.entity.Family;
import com.famora.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "finance_transactions")
public class FinanceTransaction extends BaseTimeEntity {
  
  @Id
  @GeneratedValue
  @Column(name = "id", columnDefinition = "uuid")
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "family_id", nullable = false)
  private Family family;
  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private FinanceTransactionType type;
  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;
  @Column(name = "currency", nullable = false, length = 3)
  private String currency = "MYR";
  @Column(name = "category", nullable = false, length = 100)
  private String category;
  @Column(name = "description", columnDefinition = "text")
  private String description;
  @Column(name = "transaction_date", nullable = false)
  private LocalDate transactionDate;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "updated_by")
  private User updatedBy;
  @Column(name = "deleted_at")
  private OffsetDateTime deletedAt;
}
