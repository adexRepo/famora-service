package com.famora.family.spec;

import com.famora.family.entity.FamilyMember;
import com.famora.family.helper.FamilyMemberRole;
import com.famora.family.helper.FamilyMemberStatus;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class FamilyMemberSpecifications {
  
  private FamilyMemberSpecifications() {
  }
  
  public static Specification<FamilyMember> family(UUID familyId) {
    return (root, query, cb) ->
        cb.equal(root.get("family").get("id"), familyId);
  }
  
  public static Specification<FamilyMember> status(FamilyMemberStatus status) {
    return (root, query, cb) -> {
      if (status == null) {
        return cb.conjunction();
      }
      
      return cb.equal(root.get("status"), status);
    };
  }
  
  public static Specification<FamilyMember> role(FamilyMemberRole role) {
    return (root, query, cb) -> {
      if (role == null) {
        return cb.conjunction();
      }
      
      return cb.equal(root.get("role"), role);
    };
  }
  
  public static Specification<FamilyMember> keyword(String keyword) {
    return (root, query, cb) -> {
      if (keyword == null || keyword.isBlank()) {
        return cb.conjunction();
      }
      
      String like = "%" + keyword.trim().toLowerCase() + "%";
      
      return cb.or(
          cb.like(cb.lower(root.get("user").get("fullName")), like),
          cb.like(cb.lower(root.get("user").get("email")), like)
      );
    };
  }
}
