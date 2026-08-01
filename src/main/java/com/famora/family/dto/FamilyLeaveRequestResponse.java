package com.famora.family.dto;

import com.famora.family.helper.FamilyLeaveRequestStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FamilyLeaveRequestResponse(
    UUID id,
    UUID familyId,
    UUID requesterUserId,
    String requesterName,
    FamilyLeaveRequestStatus requestStatus,
    String reason,
    String reviewReason,
    UUID reviewedByUserId,
    String reviewedByName,
    OffsetDateTime reviewedAt,
    UUID cancelledByUserId,
    String cancelledByName,
    OffsetDateTime cancelledAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
