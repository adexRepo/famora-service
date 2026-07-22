package com.famora.vault.dto;

import com.famora.common.helper.Visibility;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VaultItemResponse(
    UUID id,
    String title,
    String username,
    String url,
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
