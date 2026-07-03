package com.famora.business.spec;

import com.famora.business.entity.BusinessInvitation;
import com.famora.business.enums.InvitationStatus;
import com.famora.common.helper.Status;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class BusinessInvitationSpecifications {
  
  private BusinessInvitationSpecifications() {
  }
  
  public static Specification<BusinessInvitation> belongsToBusiness(UUID businessId) {
    return (root, query, cb) -> cb.equal(root.get("business").get("id"), businessId);
  }
  
  public static Specification<BusinessInvitation> invitationStatus(InvitationStatus status) {
    return (root, query, cb) -> cb.equal(root.get("invitationStatus"), status);
  }
  
  public static Specification<BusinessInvitation> status(Status status) {
    return (root, query, cb) -> cb.equal(root.get("status"), status);
  }
}
