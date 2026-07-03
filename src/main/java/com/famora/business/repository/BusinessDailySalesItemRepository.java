package com.famora.business.repository;

import com.famora.business.entity.BusinessDailySalesItem;
import com.famora.common.helper.Status;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessDailySalesItemRepository extends
    JpaRepository<BusinessDailySalesItem, UUID> {
  
  List<BusinessDailySalesItem> findByDailyReportIdAndStatus(UUID dailyReportId, Status status);
  
  @Query("""
        select i.itemName, sum(i.quantitySold), sum(i.totalAmount)
        from BusinessDailySalesItem i
        where i.business.id = :businessId
          and i.status = com.famora.common.helper.Status.ACTIVE
          and i.dailyReportId in (
            select r.id from BusinessDailyReport r
            where r.business.id = :businessId
            and r.reportDate between :fromDate and :toDate
            and r.reportStatus <> com.famora.business.enums.DailyReportStatus.VOIDED
          )
        group by i.itemName
        order by sum(i.totalAmount) desc
      """)
  List<Object[]> findTopSalesItems(@Param("businessId") UUID businessId,
      @Param("fromDate") LocalDate fromDate,
      @Param("toDate") LocalDate toDate, Pageable pageable);
}
