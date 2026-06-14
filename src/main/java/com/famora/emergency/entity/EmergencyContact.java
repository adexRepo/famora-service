package com.famora.emergency.entity;

import com.famora.common.entity.BaseEntity;
import com.famora.common.helper.Status;
import com.famora.emergency.helper.EmergencyCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "emergency_contacts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContact extends BaseEntity {
  
  @Id
  @GeneratedValue
  @Column(name = "id", columnDefinition = "uuid")
  private UUID id;
  @Column(nullable = false)
  private UUID familyId;
  @Column(nullable = false)
  private UUID createdByUserId;
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
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status;
  
  @PrePersist
  public void prePersist() {
    if (status == null) {
      status = Status.ACTIVE;
    }
  }
}
