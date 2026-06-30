package com.famora.business.dto.snapshot;

import com.famora.business.entity.BusinessDailyLossItem;
import java.math.BigDecimal;
import java.util.UUID;

public record DailyReportSnapshotLossItemDto(
    UUID id,
    String itemName,
    String unit,
    BigDecimal quantityLoss,
    BigDecimal estimatedUnitValue,
    BigDecimal estimatedTotalValue,
    String reason,
    String notes
) {

  public static DailyReportSnapshotLossItemDto from(BusinessDailyLossItem item) {
    return new DailyReportSnapshotLossItemDto(
        item.getId(),
        item.getItemName(),
        String.valueOf(item.getUnit()),
        item.getQuantityLoss(),
        item.getEstimatedUnitValue(),
        item.getEstimatedTotalValue(),
        String.valueOf(item.getReason()),
        item.getNotes()
    );
  }
}
