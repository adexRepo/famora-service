package com.famora.business.repository;

import com.famora.business.entity.BusinessInvitation;
import com.famora.business.enums.InvitationStatus;
import com.famora.common.helper.Status;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessInvitationRepository extends JpaRepository<BusinessInvitation, UUID>,
    JpaSpecificationExecutor<BusinessInvitation> {
  
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<BusinessInvitation> findByInvitationCodeHash(String invitationCodeHash);
  
  boolean existsByInvitationCodeHash(String invitationCodeHash);
  
  List<BusinessInvitation> findByBusinessIdAndInvitationStatusAndStatus(UUID businessId,
      InvitationStatus invitationStatus, Status status);

  @Query("""
      select invitation
      from BusinessInvitation invitation
      where invitation.business.id = :businessId
        and invitation.acceptedByUserId in :acceptedByUserIds
        and invitation.invitationStatus = :invitationStatus
        and invitation.status = :status
      order by invitation.updatedAt desc
      """)
  List<BusinessInvitation> findAcceptedByUsers(
      @Param("businessId") UUID businessId,
      @Param("acceptedByUserIds") List<UUID> acceptedByUserIds,
      @Param("invitationStatus") InvitationStatus invitationStatus,
      @Param("status") Status status);
  
}
