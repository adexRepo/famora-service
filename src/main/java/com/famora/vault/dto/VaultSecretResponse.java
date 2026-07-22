package com.famora.vault.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VaultSecretResponse(
    UUID id,
    String secret,
    VaultRevealPurpose purpose,
    OffsetDateTime revealedAt
) {

}
