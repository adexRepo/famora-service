package com.famora.admin.dto;

import java.util.UUID;

public record AdminBackupQuotaResponse(
    UUID userId,
    long defaultQuotaBytes,
    long effectiveQuotaBytes,
    long usedBytes,
    long reservedBytes,
    long availableBytes,
    boolean overrideEnabled
) {
}
