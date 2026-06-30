package com.famora.business.dto.snapshot;

import com.famora.business.entity.BusinessDailySalesItem;
import java.math.BigDecimal;
import java.util.UUID;

public record DailyReportSnapshotSalesItemDto(
    UUID id,
    UUID productId,
    String itemName,
    String unit,
    BigDecimal quantitySold,
    BigDecimal unitPrice,
    BigDecimal totalAmount,
    String notes
) {

  public static DailyReportSnapshotSalesItemDto from(BusinessDailySalesItem item) {
    return new DailyReportSnapshotSalesItemDto(
        item.getId(),
        item.getProductId(),
        item.getItemName(),
        String.valueOf(item.getUnit()),
        item.getQuantitySold(),
        item.getUnitPrice(),
        item.getTotalAmount(),
        item.getNotes()
    );
  }
}
