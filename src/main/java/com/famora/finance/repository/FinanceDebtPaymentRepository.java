package com.famora.finance.repository;

import com.famora.common.helper.Status;
import com.famora.finance.entity.FinanceDebtPayment;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FinanceDebtPaymentRepository extends JpaRepository<FinanceDebtPayment, UUID> {
  
  List<FinanceDebtPayment> findAllByDebtIdAndStatusOrderByPaymentDateAscCreatedAtAsc(UUID debtId,
      Status status);
  
  Optional<FinanceDebtPayment> findByIdAndDebtIdAndFamilyIdAndStatus(UUID id, UUID debtId,
      UUID familyId, Status status);
  
  @Query("""
        select coalesce(sum(p.amount), 0)
        from FinanceDebtPayment p
        where p.debt.id = :debtId
          and p.status = com.famora.common.helper.Status.ACTIVE
      """)
  BigDecimal sumActivePayments(UUID debtId);
}
