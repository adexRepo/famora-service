package com.famora.family.dto;

import java.util.UUID;

public record LeaveFamilyResultResponse(
    UUID leftFamilyId,
    FamilySummaryResponse newDefaultFamily
) {
}
