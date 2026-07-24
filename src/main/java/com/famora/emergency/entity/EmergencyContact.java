package com.famora.emergency.entity;

import com.famora.common.entity.FamilyScopedEntity;
import com.famora.emergency.helper.EmergencyCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "emergency_contacts")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContact extends FamilyScopedEntity {
  
  @Column(nullable = false)
  private String name;
  @Column(nullable = false)
  private String phone;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EmergencyCategory category;
  private String location;
  @Column(columnDefinition = "TEXT")
  private String notes;
}
