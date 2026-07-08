package com.famora.user.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserProfileResponse(UUID id, String fullName, String email, LocalDate dateOfBirth,
                                  String status, OffsetDateTime createdAt) {}
