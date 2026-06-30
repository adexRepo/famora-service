package com.famora.business.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RejectReportRequest(
    @NotBlank(message = "Reject reason is required")
    String reason
) {
}
