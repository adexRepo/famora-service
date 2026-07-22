package com.famora.vault.dto;

import com.famora.common.helper.Visibility;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VaultItemDetailResponse(
    UUID id,
    String title,
    String username,
    String secret,
    String url,
    String notes,
    Visibility visibility,
    UUID createdByUserId,
    String createdByName,
    boolean canViewSecret,
    boolean canUpdate,
    boolean canDelete,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
