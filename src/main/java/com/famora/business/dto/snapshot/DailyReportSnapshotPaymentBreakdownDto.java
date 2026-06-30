package com.famora.business.dto.snapshot;

import com.famora.business.entity.BusinessDailyPaymentBreakdown;
import java.math.BigDecimal;
import java.util.UUID;

public record DailyReportSnapshotPaymentBreakdownDto(
    UUID id,
    String paymentMethod,
    BigDecimal amount,
    String notes
) {

  public static DailyReportSnapshotPaymentBreakdownDto from(BusinessDailyPaymentBreakdown item) {
    return new DailyReportSnapshotPaymentBreakdownDto(
        item.getId(),
        String.valueOf(item.getPaymentMethod()),
        item.getAmount(),
        item.getNotes()
    );
  }
}
