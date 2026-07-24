package com.famora.finance.repository;

import com.famora.common.helper.Status;
import com.famora.finance.dto.CurrencyAmountProjection;
import com.famora.finance.entity.FinanceTransaction;
import com.famora.finance.entity.FinanceTransactionType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface FinanceTransactionRepository extends JpaRepository<FinanceTransaction, UUID>,
    JpaSpecificationExecutor<FinanceTransaction> {
  
  Optional<FinanceTransaction> findByIdAndFamilyIdAndStatus(
      UUID id,
      UUID familyId,
      Status status
  );
  
  List<FinanceTransaction> findAllByFamilyIdAndStatusOrderByTransactionDateAscCreatedAtAsc(
      UUID familyId,
      Status status
  );
  
  List<FinanceTransaction> findAllByFamilyIdAndStatusAndCurrencyOrderByTransactionDateAscCreatedAtAsc(
      UUID familyId,
      Status status,
      String currency
  );
  
  List<FinanceTransaction> findAllByFamilyIdAndStatusAndCurrencyAndTransactionDateBetweenOrderByTransactionDateAscCreatedAtAsc(
      UUID familyId,
      Status status,
      String currency,
      LocalDate startDate,
      LocalDate endDate
  );
  
  Optional<FinanceTransaction> findFirstByFamilyIdAndStatusAndCurrencyOrderByTransactionDateAsc(
      UUID familyId,
      Status status,
      String currency
  );
  
  @Query("""
        select
          ft.currency as currency,
          coalesce(sum(ft.amount), 0) as totalAmount
        from FinanceTransaction ft
        where ft.family.id = :familyId
          and ft.status = com.famora.common.helper.Status.ACTIVE
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
