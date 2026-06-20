package com.famora.family.repository;

import com.famora.family.entity.FamilyInvitation;
import com.famora.family.helper.InvitationStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyInvitationRepository extends JpaRepository<FamilyInvitation, UUID> {
  
  Optional<FamilyInvitation> findByInviteCodeAndStatus(String inviteCode, InvitationStatus status);
}
