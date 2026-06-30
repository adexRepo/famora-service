package com.famora.business.repository;

import com.famora.business.entity.BusinessDailyReportRevision;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessDailyReportRevisionRepository
    extends JpaRepository<BusinessDailyReportRevision, UUID>,
    JpaSpecificationExecutor<BusinessDailyReportRevision> {
  
  @Query("""
      select max(r.revisionNumber)
      from BusinessDailyReportRevision r
      where r.dailyReport.id = :dailyReportId
      """)
  Optional<Integer> findMaxRevisionNumber(@Param("dailyReportId") UUID dailyReportId);
  
  Page<BusinessDailyReportRevision> findByBusiness_IdAndDailyReport_IdOrderByRevisionNumberDesc(
      UUID businessId,
      UUID dailyReportId,
      Pageable pageable
  );
  
  Optional<BusinessDailyReportRevision> findByIdAndBusiness_IdAndDailyReport_Id(
      UUID revisionId,
      UUID businessId,
      UUID dailyReportId
  );
}
