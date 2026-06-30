package com.famora.family.entity;

import com.famora.common.entity.AuditableEntity;
import com.famora.common.entity.BaseEntity;
import com.famora.family.helper.FamilyMemberRole;
import com.famora.family.helper.FamilyMemberStatus;
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
import jakarta.persistence.PrePersist;
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
@Table(name = "family_members", uniqueConstraints = @UniqueConstraint(name = "uk_family_members_family_user", columnNames = {
    "family_id", "user_id"}))
public class FamilyMember extends BaseEntity {
  
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "family_id", nullable = false)
  private Family family;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;
  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private FamilyMemberRole role;
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private FamilyMemberStatus status;
  @Column(name = "joined_at")
  private OffsetDateTime joinedAt;
  @Column(name = "removed_at")
  private OffsetDateTime removedAt;
  
  @PrePersist
  public void prePersist() {
    if (role == null) {
      role = FamilyMemberRole.MEMBER;
    }
  }
}
