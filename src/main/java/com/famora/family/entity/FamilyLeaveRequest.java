package com.famora.family.entity;

import com.famora.common.entity.BaseEntity;
import com.famora.family.helper.FamilyLeaveRequestStatus;
import com.famora.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
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
@Table(name = "family_leave_requests")
public class FamilyLeaveRequest extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "family_id", nullable = false)
  private Family family;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "requester_user_id", nullable = false)
  private User requester;

  @Enumerated(EnumType.STRING)
  @Column(name = "request_status", nullable = false, length = 30)
  private FamilyLeaveRequestStatus requestStatus;

  @Column(name = "reason", columnDefinition = "text")
  private String reason;

  @Column(name = "review_reason", columnDefinition = "text")
  private String reviewReason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reviewed_by_user_id")
  private User reviewedBy;

  @Column(name = "reviewed_at")
  private OffsetDateTime reviewedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cancelled_by_user_id")
  private User cancelledBy;

  @Column(name = "cancelled_at")
  private OffsetDateTime cancelledAt;

  @PrePersist
  public void prePersist() {
    if (requestStatus == null) {
      requestStatus = FamilyLeaveRequestStatus.PENDING;
    }
  }
}
