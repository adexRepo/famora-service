package com.famora.family.repository;

import com.famora.family.entity.FamilyInvitation;
import com.famora.family.helper.InvitationStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface FamilyInvitationRepository extends JpaRepository<FamilyInvitation, UUID> {
  
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<FamilyInvitation> findByInviteCodeHashAndStatus(String inviteCodeHash,
      InvitationStatus status);

  boolean existsByInviteCodeHash(String inviteCodeHash);
}
