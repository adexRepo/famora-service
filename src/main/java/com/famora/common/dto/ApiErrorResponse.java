package com.famora.common.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import lombok.Builder;

@Builder
public record ApiErrorResponse(
    OffsetDateTime timestamp,
    int status,
    String error,
    String message,
    String path,
    Map<String, String> details
) {

}
