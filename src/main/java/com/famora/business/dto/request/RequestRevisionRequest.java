package com.famora.business.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RequestRevisionRequest(
    @NotBlank(message = "Revision reason is required")
    String reason
) {
}
