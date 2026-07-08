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
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {

}
