package com.famora.business.entity;

import com.famora.business.enums.BusinessRole;
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
@Table(name = "business_members")
public class BusinessMember extends BusinessScopedEntity {
  
  @Column(name = "user_id", nullable = false)
  private UUID userId;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private BusinessRole role;
  @Column(name = "invited_by_user_id")
  private UUID invitedByUserId;
  @Column(name = "joined_at")
  private LocalDateTime joinedAt;
}
