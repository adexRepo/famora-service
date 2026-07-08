package com.famora.business.repository;

import com.famora.business.entity.BusinessDailyReportPhoto;
import com.famora.common.helper.Status;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessDailyReportPhotoRepository
    extends JpaRepository<BusinessDailyReportPhoto, UUID> {
  
  List<BusinessDailyReportPhoto> findByBusiness_IdAndDailyReport_IdAndStatusOrderByCreatedAtAsc(
      UUID businessId,
      UUID dailyReportId,
      Status status
  );
  
  Optional<BusinessDailyReportPhoto> findByIdAndBusiness_IdAndDailyReport_IdAndStatus(
      UUID id,
      UUID businessId,
      UUID dailyReportId,
      Status status
  );
}
