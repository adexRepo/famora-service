package com.famora.business.dto.request;

import com.famora.business.enums.BusinessRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(@NotNull BusinessRole role) {}
