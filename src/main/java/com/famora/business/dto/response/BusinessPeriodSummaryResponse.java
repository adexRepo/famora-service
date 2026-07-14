package com.famora.business.dto.response;

import com.famora.business.enums.BusinessSummaryPeriod;
import java.time.LocalDate;

public record BusinessPeriodSummaryResponse(BusinessSummaryPeriod period,
                                            LocalDate fromDate,
                                            LocalDate toDate,
                                            BusinessSummaryResponse summary) {
}
