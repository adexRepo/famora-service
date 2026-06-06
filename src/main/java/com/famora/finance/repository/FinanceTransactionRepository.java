package com.famora.finance.repository;

import com.famora.finance.entity.FinanceTransaction;
import com.famora.finance.entity.FinanceTransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FinanceTransactionRepository extends JpaRepository<FinanceTransaction, UUID> {
  
  Optional<FinanceTransaction> findByIdAndFamilyIdAndDeletedAtIsNull(UUID id, UUID familyId);
  
  @Query("""
          select ft
          from FinanceTransaction ft
          where ft.family.id = :familyId
            and ft.deletedAt is null
            and ft.transactionDate between :startDate and :endDate
            and (:type is null or ft.type = :type)
            and (:category is null or lower(ft.category) = lower(:category))
          order by ft.transactionDate desc, ft.createdAt desc
      """)
  List<FinanceTransaction> search(
      UUID familyId,
      LocalDate startDate,
      LocalDate endDate,
      FinanceTransactionType type,
      String category
  );
  
  @Query("""
          select coalesce(sum(ft.amount), 0)
          from FinanceTransaction ft
          where ft.family.id = :familyId
            and ft.deletedAt is null
            and ft.transactionDate between :startDate and :endDate
            and ft.type = :type
            and ft.currency = :currency
      """)
  BigDecimal sumAmountByType(
      UUID familyId,
      LocalDate startDate,
      LocalDate endDate,
      FinanceTransactionType type,
      String currency
  );
}
