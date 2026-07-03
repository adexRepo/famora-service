package com.famora.family.dto;

import com.famora.family.helper.FamilyMemberRole;
import com.famora.family.helper.FamilyMemberStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FamilyMemberResponse(
    UUID id,
    UUID userId,
    String fullName,
    String email,
    FamilyMemberRole role,
    FamilyMemberStatus status,
    boolean isDefault,
    OffsetDateTime joinedAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
