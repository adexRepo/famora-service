package com.famora.user.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserProfileResponse(UUID id, String fullName, String email, String status, OffsetDateTime createdAt) {}
