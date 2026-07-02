package com.famora.business.dto.response;

import com.famora.business.entity.BusinessDailyReportRevision;
import com.famora.business.enums.BusinessDailyReportRevisionChangeType;
import com.famora.business.enums.DailyReportStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DailyReportRevisionListResponse(
    UUID id,
    Integer revisionNumber,
    BusinessDailyReportRevisionChangeType changeType,
    DailyReportStatus oldStatus,
    DailyReportStatus newStatus,
    UUID changedByUserId,
    String changeReason,
    OffsetDateTime createdAt
) {

  public static DailyReportRevisionListResponse from(BusinessDailyReportRevision revision) {
    return new DailyReportRevisionListResponse(
        revision.getId(),
        revision.getRevisionNumber(),
        revision.getChangeType(),
        revision.getOldStatus(),
        revision.getNewStatus(),
        revision.getChangedByUserId(),
        revision.getChangeReason(),
        revision.getCreatedAt()
    );
  }
}
