package com.famora.business.repository;

import com.famora.business.entity.BusinessInvitation;
import com.famora.business.enums.InvitationStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BusinessInvitationRepository extends JpaRepository<BusinessInvitation, UUID>,
    JpaSpecificationExecutor<BusinessInvitation> {
  
  Optional<BusinessInvitation> findByInvitationCode(String invitationCode);
  
  boolean existsByInvitationCode(String invitationCode);
  
}
