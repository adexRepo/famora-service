package com.famora.vault.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VaultItemDetailResponse(
    UUID id,
    String title,
    String username,
    String secret,
    String url,
    String notes,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
