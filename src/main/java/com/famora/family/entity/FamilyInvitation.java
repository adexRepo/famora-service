package com.famora.family.entity;

import com.famora.common.entity.BaseTimeEntity;
import com.famora.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "family_invitations", uniqueConstraints = @UniqueConstraint(name = "uk_family_invitations_invite_code", columnNames = "invite_code"))
public class FamilyInvitation extends BaseTimeEntity {
  
  @Id
  @GeneratedValue
  @Column(name = "id", columnDefinition = "uuid")
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "family_id", nullable = false)
  private Family family;
  @Column(name = "invite_code", nullable = false, length = 50)
  private String inviteCode;
  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private FamilyMemberRole role = FamilyMemberRole.MEMBER;
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private InvitationStatus status = InvitationStatus.ACTIVE;
  @Column(name = "expires_at", nullable = false)
  private OffsetDateTime expiresAt;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "used_by_user_id")
  private User usedByUser;
  @Column(name = "used_at")
  private OffsetDateTime usedAt;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;
}
