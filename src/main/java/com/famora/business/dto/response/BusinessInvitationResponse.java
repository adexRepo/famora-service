package com.famora.business.dto.response;

import com.famora.business.enums.BusinessRole;
import com.famora.business.enums.InvitationStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record BusinessInvitationResponse(UUID id, UUID businessId, String invitedEmail, String invitedPhone,
                                         BusinessRole role, InvitationStatus invitationStatus,
                                         LocalDateTime expiresAt, UUID invitedByUserId, UUID acceptedByUserId) {}
