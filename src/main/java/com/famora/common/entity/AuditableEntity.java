package com.famora.common.entity;

import com.famora.common.helper.Status;
import com.famora.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class AuditableEntity extends BaseEntity {
  
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by", nullable = false, updatable = false)
  private User createdBy;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "updated_by")
  private User updatedBy;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private Status status;
  
  @PrePersist
  protected void onAuditableCreated() {
    if (this.status == null) {
      this.status = Status.ACTIVE;
    }
  }
}
