package com.famora.business.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BusinessDashboardSummaryResponse(UUID businessId,
                                               String timezone,
                                               LocalDate anchorDate,
                                               List<BusinessPeriodSummaryResponse> summaries) {
}
