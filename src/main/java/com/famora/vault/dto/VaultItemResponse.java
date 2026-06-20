package com.famora.vault.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VaultItemResponse(
    UUID id,
    String title,
    String username,
    String url,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {

}
