package com.famora.business.dto.request;

import jakarta.validation.constraints.NotBlank;

public record JoinBusinessRequest(@NotBlank String invitationCode) {}
