package com.famora.business.entity;

import com.famora.business.constant.BusinessDefaults;
import com.famora.business.enums.LossReason;
import com.famora.common.entity.BusinessScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "business_daily_loss_items")
public class BusinessDailyLossItem extends BusinessScopedEntity {
  
  @Column(name = "daily_report_id", nullable = false)
  private UUID dailyReportId;
  @Column(name = "item_name", nullable = false, length = 150)
  private String itemName;
  @Column(nullable = false, length = 50)
  private String unit = BusinessDefaults.UNIT;
  @Column(name = "quantity_loss", nullable = false, precision = 18, scale = 2)
  private BigDecimal quantityLoss;
  @Column(name = "estimated_unit_value", nullable = false, precision = 18, scale = 2)
  private BigDecimal estimatedUnitValue = BigDecimal.ZERO;
  @Column(name = "estimated_total_value", nullable = false, precision = 18, scale = 2)
  private BigDecimal estimatedTotalValue = BigDecimal.ZERO;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 80)
  private LossReason reason = LossReason.UNSOLD;
  @Column(columnDefinition = "text")
  private String notes;
}
