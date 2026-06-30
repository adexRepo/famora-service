package com.famora.business.spec;

import com.famora.business.entity.Business;
import com.famora.business.entity.BusinessMember;
import com.famora.common.helper.Status;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class BusinessSpecifications {
  
  private BusinessSpecifications() {
  }
  
  public static Specification<Business> accessibleByUser(UUID userId) {
    return (root, query, cb) -> {
      var memberSubquery = query.subquery(Integer.class);
      var member = memberSubquery.from(BusinessMember.class);
      memberSubquery.select(cb.literal(1))
          .where(
              cb.equal(member.get("business").get("id"), root.get("id")),
              cb.equal(member.get("userId"), userId),
              cb.equal(member.get("status"), Status.ACTIVE)
          );
      
      return cb.and(
          cb.notEqual(root.get("status"), Status.DELETED),
          cb.exists(memberSubquery)
      );
    };
  }
}
