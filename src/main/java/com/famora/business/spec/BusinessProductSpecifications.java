package com.famora.business.spec;

import com.famora.business.entity.BusinessProduct;
import com.famora.common.helper.Status;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class BusinessProductSpecifications {
  
  private BusinessProductSpecifications() {
  }
  
  public static Specification<BusinessProduct> belongsToBusiness(UUID businessId) {
    return (root, query, cb) -> cb.equal(root.get("business").get("id"), businessId);
  }
  
  public static Specification<BusinessProduct> statusNot(Status status) {
    return (root, query, cb) -> cb.notEqual(root.get("status"), status);
  }
}
