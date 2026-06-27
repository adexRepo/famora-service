package com.famora.common.spec;

import com.famora.common.helper.Status;
import com.famora.common.helper.Visibility;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class VisibleFamilyScopedSpecifications {
  
  private VisibleFamilyScopedSpecifications() {
  }
  
  public static <T> Specification<T> visibleToUser(
      UUID familyId,
      UUID userId,
      boolean isOwner,
      Status status,
      Visibility visibility
  ) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      
      predicates.add(cb.equal(root.get("family").get("id"), familyId));
      predicates.add(cb.equal(root.get("status"), status));
      
      if (visibility != null) {
        predicates.add(cb.equal(root.get("visibility"), visibility));
      }
      
      Predicate familyVisible = cb.equal(root.get("visibility"), Visibility.FAMILY);
      
      Predicate privateVisible = cb.and(
          cb.equal(root.get("visibility"), Visibility.PRIVATE),
          cb.equal(root.get("createdBy").get("id"), userId)
      );
      
      Predicate ownerOnlyVisible = cb.and(
          cb.isTrue(cb.literal(isOwner)),
          cb.equal(root.get("visibility"), Visibility.OWNER_ONLY)
      );
      
      predicates.add(cb.or(
          familyVisible,
          privateVisible,
          ownerOnlyVisible
      ));
      
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
