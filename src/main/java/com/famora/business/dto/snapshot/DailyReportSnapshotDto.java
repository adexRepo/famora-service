package com.famora.business.dto.snapshot;

import java.util.List;

public record DailyReportSnapshotDto(
    DailyReportSnapshotHeaderDto header,
    List<DailyReportSnapshotSalesItemDto> salesItems,
    List<DailyReportSnapshotPaymentBreakdownDto> paymentBreakdowns,
    List<DailyReportSnapshotLossItemDto> lossItems,
    List<DailyReportSnapshotExpenseDto> expenses
) {
}
