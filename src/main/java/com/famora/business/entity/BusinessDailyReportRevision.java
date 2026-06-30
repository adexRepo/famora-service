package com.famora.business.entity;

import com.famora.business.enums.BusinessDailyReportRevisionChangeType;
import com.famora.business.enums.DailyReportStatus;
import com.famora.common.entity.BusinessScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "business_daily_report_revisions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "ux_business_daily_report_revisions_report_number",
            columnNames = {"daily_report_id", "revision_number"}
        )
    }
)
public class BusinessDailyReportRevision extends BusinessScopedEntity {
  
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "daily_report_id", nullable = false)
  private BusinessDailyReport dailyReport;
  
  @Column(name = "revision_number", nullable = false)
  private Integer revisionNumber;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "change_type", nullable = false, length = 80)
  private BusinessDailyReportRevisionChangeType changeType;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "old_status", length = 30)
  private DailyReportStatus oldStatus;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "new_status", length = 30)
  private DailyReportStatus newStatus;
  
  @Column(name = "changed_by_user_id", nullable = false)
  private UUID changedByUserId;
  
  @Column(name = "change_reason")
  private String changeReason;
  
  @Column(name = "snapshot_json", nullable = false, columnDefinition = "text")
  private String snapshotJson;
}
