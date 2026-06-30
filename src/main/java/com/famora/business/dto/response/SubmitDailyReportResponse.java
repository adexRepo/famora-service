package com.famora.business.dto.response;

import com.famora.business.enums.DailyReportStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SubmitDailyReportResponse(
    UUID reportId,
    UUID businessId,
    DailyReportStatus oldStatus,
    DailyReportStatus newStatus,
    Integer revisionNumber,
    OffsetDateTime submittedAt,
    String message
) {
}
