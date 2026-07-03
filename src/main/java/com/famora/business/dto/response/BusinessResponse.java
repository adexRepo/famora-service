package com.famora.business.dto.response;

import com.famora.common.helper.Status;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BusinessResponse(UUID id, String name, String businessType, String defaultCurrency,
                               UUID ownerUserId, UUID primaryFamilyId, String description,
                               Status status, boolean isDefault, OffsetDateTime createdAt,
                               OffsetDateTime updatedAt) {
  
}
