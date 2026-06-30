package com.famora.business.entity;

import com.famora.business.constant.BusinessDefaults;
import com.famora.common.entity.BusinessScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "business_daily_sales_items")
public class BusinessDailySalesItem extends BusinessScopedEntity {
  
  @Column(name = "daily_report_id", nullable = false)
  private UUID dailyReportId;
  @Column(name = "product_id")
  private UUID productId;
  @Column(name = "item_name", nullable = false, length = 150)
  private String itemName;
  @Column(nullable = false, length = 50)
  private String unit = BusinessDefaults.UNIT;
  @Column(name = "quantity_sold", nullable = false, precision = 18, scale = 2)
  private BigDecimal quantitySold;
  @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
  private BigDecimal unitPrice;
  @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalAmount;
  @Column(columnDefinition = "text")
  private String notes;
}
