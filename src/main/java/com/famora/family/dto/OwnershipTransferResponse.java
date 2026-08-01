package com.famora.family.dto;

import com.famora.family.helper.FamilyMemberRole;
import java.util.UUID;

public record OwnershipTransferResponse(
    UUID familyId,
    UUID oldOwnerUserId,
    UUID newOwnerUserId,
    FamilyMemberRole oldOwnerRole,
    boolean oldOwnerLeftFamily,
    FamilySummaryResponse newDefaultFamily
) {
}
