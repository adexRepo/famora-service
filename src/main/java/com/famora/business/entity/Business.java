package com.famora.business.entity;

import com.famora.business.constant.BusinessDefaults;
import com.famora.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "businesses")
public class Business extends AuditableEntity {
  
  @Column(nullable = false, length = 150)
  private String name;
  @Column(name = "business_type", nullable = false, length = 80)
  private String businessType = BusinessDefaults.BUSINESS_TYPE;
  @Column(name = "default_currency", nullable = false, length = 10)
  private String defaultCurrency = BusinessDefaults.CURRENCY;
  @Column(name = "owner_user_id", nullable = false)
  private UUID ownerUserId;
  @Column(name = "primary_family_id")
  private UUID primaryFamilyId;
  @Column(columnDefinition = "text")
  private String description;
  @Column(columnDefinition = "text")
  private String address;
  @Column(length = 80)
  private String contact;
}
