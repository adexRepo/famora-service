package com.famora.family.dto;

import java.time.OffsetDateTime;

public record InvitationResponse(String inviteCode, OffsetDateTime expiresAt, String role) {}
