package com.famora.business.dto.response;

import com.famora.business.enums.BusinessRole;
import com.famora.common.helper.Status;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BusinessResponse(UUID id, String name, String businessType, String defaultCurrency,
                               UUID ownerUserId, UUID primaryFamilyId, String description,
                               String address, String contact,
                               Status status, boolean isDefault, BusinessRole role,
                               OffsetDateTime createdAt,
                               OffsetDateTime updatedAt) {
  
}
