package com.famora.business.spec;

import com.famora.business.entity.BusinessMember;
import com.famora.common.helper.Status;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class BusinessMemberSpecifications {
  
  private BusinessMemberSpecifications() {
  }
  
  public static Specification<BusinessMember> belongsToBusiness(UUID businessId) {
    return (root, query, cb) -> cb.equal(root.get("business").get("id"), businessId);
  }
  
  public static Specification<BusinessMember> status(Status status) {
    return (root, query, cb) -> cb.equal(root.get("status"), status);
  }
}
