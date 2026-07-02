package com.famora.business.dto.response;

import com.famora.business.entity.BusinessDailyReportRevision;
import com.famora.business.enums.BusinessDailyReportRevisionChangeType;
import com.famora.business.enums.DailyReportStatus;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DailyReportRevisionDetailResponse(
    UUID id,
    UUID reportId,
    UUID businessId,
    Integer revisionNumber,
    BusinessDailyReportRevisionChangeType changeType,
    DailyReportStatus oldStatus,
    DailyReportStatus newStatus,
    UUID changedByUserId,
    String changeReason,
    OffsetDateTime createdAt,
    JsonNode snapshot
) {

  public static DailyReportRevisionDetailResponse from(
      BusinessDailyReportRevision revision,
      JsonNode snapshot
  ) {
    return new DailyReportRevisionDetailResponse(
        revision.getId(),
        revision.getDailyReport().getId(),
        revision.getBusiness().getId(),
        revision.getRevisionNumber(),
        revision.getChangeType(),
        revision.getOldStatus(),
        revision.getNewStatus(),
        revision.getChangedByUserId(),
        revision.getChangeReason(),
        revision.getCreatedAt(),
        snapshot
    );
  }
}
