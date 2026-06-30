package com.famora.business.repository;

import com.famora.business.entity.BusinessDailyLossItem;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessDailyLossItemRepository extends
    JpaRepository<BusinessDailyLossItem, UUID> {
  
  List<BusinessDailyLossItem> findByDailyReportId(UUID dailyReportId);
  
  @Query("""
        select i.reason, sum(i.quantityLoss), sum(i.estimatedTotalValue)
        from BusinessDailyLossItem i
        where i.business.id = :businessId
          and i.dailyReportId in (
            select r.id from BusinessDailyReport r
            where r.business.id = :businessId
            and r.reportDate between :fromDate and :toDate
            and r.reportStatus <> com.famora.business.enums.DailyReportStatus.VOIDED
          )
        group by i.reason
        order by sum(i.estimatedTotalValue) desc
      """)
  List<Object[]> summarizeLoss(@Param("businessId") UUID businessId,
      @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}
