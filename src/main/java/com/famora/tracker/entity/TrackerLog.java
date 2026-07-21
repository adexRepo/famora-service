package com.famora.tracker.entity;

import com.famora.business.entity.Business;
import com.famora.common.entity.BaseEntity;
import com.famora.family.entity.Family;
import com.famora.tracker.enums.TrackerLogStatus;
import com.famora.tracker.enums.TrackerScopeType;
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
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tracker_logs")
public class TrackerLog extends BaseEntity {
  
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tracker_id", nullable = false)
  private Tracker tracker;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "scope_type", nullable = false, length = 30)
  private TrackerScopeType scopeType;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "family_id")
  private Family family;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "business_id")
  private Business business;
  
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "logged_by_user_id", nullable = false)
  private User loggedByUser;
  
  @Column(name = "log_date", nullable = false)
  private LocalDate logDate;
  
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TrackerLogStatus status;
  
  @Column(length = 100)
  private String value;
  
  @Column(columnDefinition = "text")
  private String notes;
  
  @PrePersist
  public void applyDefaults() {
    if (status == null) {
      status = TrackerLogStatus.PENDING;
    }
  }
}
