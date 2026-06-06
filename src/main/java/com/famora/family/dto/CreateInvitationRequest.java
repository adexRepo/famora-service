package com.famora.family.dto;

import com.famora.family.entity.FamilyMemberRole;
import jakarta.validation.constraints.NotNull;

public record CreateInvitationRequest(@NotNull FamilyMemberRole role) {}
