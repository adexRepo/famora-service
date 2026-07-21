package com.famora.tracker.spec;

import com.famora.tracker.entity.TrackerLog;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class TrackerLogSpecifications {
  
  private TrackerLogSpecifications() {
  }
  
  public static Specification<TrackerLog> tracker(UUID trackerId) {
    return (root, query, cb) -> cb.equal(root.get("tracker").get("id"), trackerId);
  }
  
  public static Specification<TrackerLog> logDateBetween(LocalDate fromDate, LocalDate toDate) {
    return (root, query, cb) -> {
      if (fromDate == null && toDate == null) {
        return cb.conjunction();
      }
      if (fromDate != null && toDate != null) {
        return cb.between(root.get("logDate"), fromDate, toDate);
      }
      if (fromDate != null) {
        return cb.greaterThanOrEqualTo(root.get("logDate"), fromDate);
      }
      return cb.lessThanOrEqualTo(root.get("logDate"), toDate);
    };
  }
}
