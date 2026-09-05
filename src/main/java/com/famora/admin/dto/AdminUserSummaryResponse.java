package com.famora.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserSummaryResponse(
    UUID userId,
    String fullName,
    String email,
    String status,
    String role,
    OffsetDateTime createdAt,
    OffsetDateTime lastLoginAt,
    long activeFamilyCount,
    Integer maxFamilyCount,
    boolean familyLimitOverrideEnabled
) {

}
