package com.famora.business.spec;

import com.famora.business.entity.BusinessDailyReport;
import com.famora.business.enums.DailyReportStatus;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class BusinessDailyReportSpecifications {
  
  private BusinessDailyReportSpecifications() {
  }
  
  public static Specification<BusinessDailyReport> belongsToBusiness(UUID businessId) {
    return (root, query, cb) -> cb.equal(root.get("business").get("id"), businessId);
  }
  
  public static Specification<BusinessDailyReport> reportDateBetween(LocalDate fromDate,
      LocalDate toDate) {
    return (root, query, cb) -> cb.between(root.get("reportDate"), fromDate, toDate);
  }
  
  public static Specification<BusinessDailyReport> reportStatusNot(DailyReportStatus status) {
    return (root, query, cb) -> cb.notEqual(root.get("reportStatus"), status);
  }
}
