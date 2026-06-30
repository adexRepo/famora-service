package com.famora.business.repository;

import com.famora.business.entity.BusinessExpense;
import com.famora.common.helper.Status;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessExpenseRepository extends JpaRepository<BusinessExpense, UUID>,
    JpaSpecificationExecutor<BusinessExpense> {
  
  Optional<BusinessExpense> findByIdAndBusinessIdAndStatusNot(UUID id, UUID businessId,
      Status status);
  
  List<BusinessExpense> findByDailyReportIdAndStatus(UUID dailyReportId, Status status);
  
  Page<BusinessExpense> findByBusinessIdAndStatusNot(UUID businessId, Status status,
      Pageable pageable);
  
  List<BusinessExpense> findByBusinessIdAndExpenseDateBetweenAndStatus(UUID businessId,
      LocalDate from, LocalDate to, Status status);
  
  @Query("""
        select e.category, sum(e.amount)
        from BusinessExpense e
        where e.business.id = :businessId
          and e.expenseDate between :fromDate and :toDate
          and e.status = com.famora.common.helper.Status.ACTIVE
        group by e.category
        order by sum(e.amount) desc
      """)
  List<Object[]> summarizeByCategory(@Param("businessId") UUID businessId,
      @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}
