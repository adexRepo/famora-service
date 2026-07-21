package com.famora.tracker.spec;

import com.famora.tracker.entity.Tracker;
import com.famora.tracker.enums.TrackerCategory;
import com.famora.tracker.enums.TrackerScopeType;
import com.famora.tracker.enums.TrackerSourceModule;
import com.famora.tracker.enums.TrackerStatus;
import com.famora.tracker.enums.TrackerType;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class TrackerSpecifications {
  
  private TrackerSpecifications() {
  }
  
  public static Specification<Tracker> accessibleToUser(UUID userId) {
    return (root, query, cb) -> cb.or(
        cb.equal(root.get("ownerUser").get("id"), userId),
        cb.equal(root.get("assignedUser").get("id"), userId)
    );
  }
  
  public static Specification<Tracker> scope(TrackerScopeType scopeType, UUID scopeId) {
    return (root, query, cb) -> {
      if (scopeType == null) {
        return cb.conjunction();
      }
      var predicate = cb.equal(root.get("scopeType"), scopeType);
      if (scopeId == null) {
        return predicate;
      }
      return switch (scopeType) {
        case FAMILY -> cb.and(predicate, cb.equal(root.get("family").get("id"), scopeId));
        case BUSINESS -> cb.and(predicate, cb.equal(root.get("business").get("id"), scopeId));
        case PERSONAL -> predicate;
      };
    };
  }
  
  public static Specification<Tracker> status(TrackerStatus status) {
    return (root, query, cb) -> status == null
        ? cb.notEqual(root.get("status"), TrackerStatus.DELETED)
        : cb.equal(root.get("status"), status);
  }
  
  public static Specification<Tracker> type(TrackerType trackerType) {
    return (root, query, cb) -> trackerType == null ? cb.conjunction()
        : cb.equal(root.get("trackerType"), trackerType);
  }
  
  public static Specification<Tracker> category(TrackerCategory category) {
    return (root, query, cb) -> category == null ? cb.conjunction()
        : cb.equal(root.get("category"), category);
  }
  
  public static Specification<Tracker> sourceModule(TrackerSourceModule sourceModule) {
    return (root, query, cb) -> sourceModule == null ? cb.conjunction()
        : cb.equal(root.get("sourceModule"), sourceModule);
  }
  
  public static Specification<Tracker> startsOnOrBefore(LocalDate date) {
    return (root, query, cb) -> date == null ? cb.conjunction()
        : cb.lessThanOrEqualTo(root.get("startDate"), date);
  }
}
