package com.famora.business.entity;

import com.famora.business.enums.BusinessRole;
import com.famora.business.enums.InvitationStatus;
import com.famora.common.entity.BusinessScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "business_invitations")
public class BusinessInvitation extends BusinessScopedEntity {
  
  @Column(name = "invited_email", length = 150)
  private String invitedEmail;
  @Column(name = "invited_phone", length = 50)
  private String invitedPhone;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private BusinessRole role;
  @Column(name = "invitation_code", nullable = false, length = 100, unique = true)
  private String invitationCode;
  @Enumerated(EnumType.STRING)
  @Column(name = "invitation_status", nullable = false, length = 30)
  private InvitationStatus invitationStatus = InvitationStatus.PENDING;
  @Column(name = "expires_at")
  private LocalDateTime expiresAt;
  @Column(name = "invited_by_user_id", nullable = false)
  private UUID invitedByUserId;
  @Column(name = "accepted_by_user_id")
  private UUID acceptedByUserId;
}
