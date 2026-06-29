package com.famora.family.dto;

import com.famora.family.helper.FamilyMemberRole;
import java.util.UUID;

public record FamilyResponse(UUID id, String name, FamilyMemberRole role, int memberCount) {}
