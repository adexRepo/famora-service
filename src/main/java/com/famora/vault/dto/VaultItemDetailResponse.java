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
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
