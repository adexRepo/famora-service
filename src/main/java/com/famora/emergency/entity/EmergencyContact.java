package com.famora.emergency.entity;

import com.famora.common.entity.FamilyScopedEntity;
import com.famora.emergency.helper.EmergencyCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
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
  
  @Id
  @GeneratedValue
  @Column(name = "id", columnDefinition = "uuid")
  private UUID id;
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
