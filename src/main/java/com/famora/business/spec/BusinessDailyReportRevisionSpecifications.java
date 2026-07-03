package com.famora.business.spec;

import com.famora.business.entity.BusinessDailyReportRevision;
import com.famora.common.helper.Status;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class BusinessDailyReportRevisionSpecifications {
  
  private BusinessDailyReportRevisionSpecifications() {
  }
  
  public static Specification<BusinessDailyReportRevision> belongsToBusiness(UUID businessId) {
    return (root, query, cb) -> cb.equal(root.get("business").get("id"), businessId);
  }
  
  public static Specification<BusinessDailyReportRevision> belongsToReport(UUID reportId) {
    return (root, query, cb) -> cb.equal(root.get("dailyReport").get("id"), reportId);
  }
  
  public static Specification<BusinessDailyReportRevision> status(Status status) {
    return (root, query, cb) -> cb.equal(root.get("status"), status);
  }
}
