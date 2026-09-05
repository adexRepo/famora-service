package com.famora.admin.dto;

import java.util.UUID;

public record AdminFamilyLimitResponse(
    UUID userId,
    int defaultMaxFamilies,
    Integer effectiveMaxFamilies,
    long activeFamilyCount,
    boolean canCreateOrJoinFamily,
    boolean overrideEnabled
) {

}
