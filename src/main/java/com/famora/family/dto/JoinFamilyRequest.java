package com.famora.family.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinFamilyRequest(@NotBlank String inviteCode) {}
