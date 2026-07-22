package com.famora.business.repository;

import com.famora.business.entity.BusinessInvitation;
import com.famora.business.enums.InvitationStatus;
import com.famora.common.helper.Status;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BusinessInvitationRepository extends JpaRepository<BusinessInvitation, UUID>,
    JpaSpecificationExecutor<BusinessInvitation> {
  
  Optional<BusinessInvitation> findByInvitationCode(String invitationCode);
  
  boolean existsByInvitationCode(String invitationCode);
  
  List<BusinessInvitation> findByBusinessIdAndInvitationStatusAndStatus(UUID businessId,
      InvitationStatus invitationStatus, Status status);
  
}
