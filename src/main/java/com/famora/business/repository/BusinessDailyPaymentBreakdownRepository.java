package com.famora.business.repository;

import com.famora.business.entity.BusinessDailyPaymentBreakdown;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessDailyPaymentBreakdownRepository extends
    JpaRepository<BusinessDailyPaymentBreakdown, UUID> {
  
  List<BusinessDailyPaymentBreakdown> findByDailyReportId(UUID dailyReportId);
}
