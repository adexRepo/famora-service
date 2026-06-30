package com.famora.business.entity;

import com.famora.business.constant.BusinessDefaults;
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
@Table(name = "business_products")
public class BusinessProduct extends BusinessScopedEntity {
  
  @Column(name = "product_name", nullable = false, length = 150)
  private String productName;
  @Column(length = 80)
  private String category;
  @Column(nullable = false, length = 50)
  private String unit = BusinessDefaults.UNIT;
  @Column(name = "default_selling_price", nullable = false, precision = 18, scale = 2)
  private BigDecimal defaultSellingPrice = BigDecimal.ZERO;
  @Column(name = "cost_price", precision = 18, scale = 2)
  private BigDecimal costPrice;
}
