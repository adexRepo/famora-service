package com.famora.business.repository;

import com.famora.business.entity.BusinessDailyReport;
import com.famora.business.enums.DailyReportStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessDailyReportRepository extends JpaRepository<BusinessDailyReport, UUID>,
    JpaSpecificationExecutor<BusinessDailyReport> {
  
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select r
      from BusinessDailyReport r
      join fetch r.business b
      where r.id = :reportId
        and b.id = :businessId
      """)
  Optional<BusinessDailyReport> findByIdAndBusinessIdForUpdate(
      @Param("businessId") UUID businessId,
      @Param("reportId") UUID reportId
  );
  
  @Query("""
      select r
      from BusinessDailyReport r
      join fetch r.business b
      where r.id = :reportId
        and b.id = :businessId
      """)
  Optional<BusinessDailyReport> findByIdAndBusinessId(
      @Param("businessId") UUID businessId,
      @Param("reportId") UUID reportId
  );
  
  
  Optional<BusinessDailyReport> findByIdAndBusinessIdAndReportStatusNot(UUID id, UUID businessId,
      DailyReportStatus status);
  
  boolean existsByBusinessIdAndReportDateAndShiftAndReportStatusNot(UUID businessId, LocalDate date,
      String shift, DailyReportStatus status);
  
  Page<BusinessDailyReport> findByBusinessIdAndReportStatusNot(UUID businessId, DailyReportStatus status,
      Pageable pageable);
  
  List<BusinessDailyReport> findByBusinessIdAndReportDateBetweenAndReportStatusNot(UUID businessId,
      LocalDate from, LocalDate to, DailyReportStatus status);
}
