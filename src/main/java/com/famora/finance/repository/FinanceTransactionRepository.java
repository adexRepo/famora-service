package com.famora.finance.repository;

import com.famora.finance.dto.CurrencyAmountProjection;
import com.famora.finance.entity.FinanceTransaction;
import com.famora.finance.entity.FinanceTransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FinanceTransactionRepository extends JpaRepository<FinanceTransaction, UUID> {
  
  Optional<FinanceTransaction> findByIdAndFamilyIdAndDeletedAtIsNull(
      UUID id,
      UUID familyId
  );
  
  Page<FinanceTransaction> findByFamily_IdAndDeletedAtIsNullAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
      UUID familyId,
      LocalDate startDate,
      LocalDate endDate,
      Pageable pageable
  );
  
  Page<FinanceTransaction> findByFamily_IdAndDeletedAtIsNullAndTransactionDateBetweenAndTypeOrderByTransactionDateDescCreatedAtDesc(
      UUID familyId,
      LocalDate startDate,
      LocalDate endDate,
      FinanceTransactionType type,
      Pageable pageable
  );
  
  Page<FinanceTransaction> findByFamily_IdAndDeletedAtIsNullAndTransactionDateBetweenAndCategoryIgnoreCaseOrderByTransactionDateDescCreatedAtDesc(
      UUID familyId,
      LocalDate startDate,
      LocalDate endDate,
      String category,
      Pageable pageable
  );
  
  Page<FinanceTransaction> findByFamily_IdAndDeletedAtIsNullAndTransactionDateBetweenAndTypeAndCategoryIgnoreCaseOrderByTransactionDateDescCreatedAtDesc(
      UUID familyId,
      LocalDate startDate,
      LocalDate endDate,
      FinanceTransactionType type,
      String category,
      Pageable pageable
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
  
  @Query("""
        select
          ft.currency as currency,
          coalesce(sum(ft.amount), 0) as totalAmount
        from FinanceTransaction ft
        where ft.family.id = :familyId
          and ft.deletedAt is null
          and ft.transactionDate between :startDate and :endDate
          and ft.type = :type
        group by ft.currency
      """)
  List<CurrencyAmountProjection> sumAmountByTypeGroupByCurrency(
      UUID familyId,
      LocalDate startDate,
      LocalDate endDate,
      FinanceTransactionType type
  );
}
