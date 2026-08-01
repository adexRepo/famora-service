package com.famora.family.dto;

import jakarta.validation.constraints.Size;

public record LeaveFamilyRequest(
    @Size(max = 1000) String reason
) {
}
