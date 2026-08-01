package com.famora.family.dto;

public record FamilyMembershipSummaryResponse(
    long activeFamilyCount,
    Integer maxFamilyCount,
    boolean canCreateOrJoinFamily,
    boolean limitOverrideEnabled
) {
}
