package com.famora.business.repository;

import com.famora.business.entity.BusinessDailyPaymentBreakdown;
import com.famora.common.helper.Status;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessDailyPaymentBreakdownRepository extends
    JpaRepository<BusinessDailyPaymentBreakdown, UUID> {
  
  List<BusinessDailyPaymentBreakdown> findByDailyReportIdAndStatus(UUID dailyReportId,
      Status status);
}
