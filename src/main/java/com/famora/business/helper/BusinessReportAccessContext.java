package com.famora.business.helper;

import com.famora.business.enums.BusinessRole;
import java.util.UUID;

public record BusinessReportAccessContext(
    UUID businessId,
    UUID userId,
    BusinessRole role
) {
}
