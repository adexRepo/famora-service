package com.famora.business.dto.response;

import com.famora.business.enums.BusinessRole;
import com.famora.common.helper.Status;
import java.time.LocalDateTime;
import java.util.UUID;

public record BusinessMemberResponse(UUID id, UUID businessId, UUID userId, BusinessRole role,
                                     Status status, boolean isDefault, UUID invitedByUserId,
                                     LocalDateTime joinedAt) {}
