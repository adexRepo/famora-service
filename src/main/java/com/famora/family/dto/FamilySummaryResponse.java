package com.famora.family.dto;

import java.util.UUID;

public record FamilySummaryResponse(
    UUID id,
    String name,
    boolean isDefault
) {
}
