package com.famora.common.entity;

import com.famora.common.helper.Visibility;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class VisibleFamilyScopedEntity extends FamilyScopedEntity {
  
  @Enumerated(EnumType.STRING)
  @Column(name = "visibility", nullable = false, length = 30)
  private Visibility visibility;
  
  @PrePersist
  protected void onVisibleCreated() {
    if (this.visibility == null) {
      this.visibility = Visibility.PRIVATE;
    }
  }
}
