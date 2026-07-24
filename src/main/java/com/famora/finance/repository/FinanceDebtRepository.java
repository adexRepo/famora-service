package com.famora.finance.repository;

import com.famora.common.helper.Status;
import com.famora.finance.entity.FinanceDebt;
import com.famora.finance.helper.FinanceDebtStatus;
import com.famora.finance.helper.FinanceDebtType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface FinanceDebtRepository extends JpaRepository<FinanceDebt, UUID>,
    JpaSpecificationExecutor<FinanceDebt> {
  
  Optional<FinanceDebt> findByIdAndFamilyIdAndStatus(UUID id, UUID familyId, Status status);
  
  List<FinanceDebt> findAllByFamilyIdAndStatus(UUID familyId, Status status);
  
  @Query("""
        select coalesce(sum(d.remainingAmount), 0)
        from FinanceDebt d
        where d.family.id = :familyId
          and d.status = com.famora.common.helper.Status.ACTIVE
          and d.debtStatus <> com.famora.finance.helper.FinanceDebtStatus.CANCELLED
          and d.debtType = :debtType
          and d.currency = :currency
      """)
  BigDecimal sumRemainingByTypeAndCurrency(UUID familyId, FinanceDebtType debtType,
      String currency);
  
  long countByFamilyIdAndStatusAndDebtStatus(UUID familyId, Status status,
      FinanceDebtStatus debtStatus);
}
