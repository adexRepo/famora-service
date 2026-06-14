package com.famora.document.entity;

import com.famora.common.entity.BaseEntity;
import com.famora.common.exception.Visibility;
import com.famora.common.helper.Status;
import com.famora.document.helper.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document extends BaseEntity {
  
  @Id
  @GeneratedValue
  @Column(name = "id", columnDefinition = "uuid")
  private UUID id;
  @Column(nullable = false)
  private UUID familyId;
  @Column(nullable = false)
  private UUID fileId;
  private UUID ownerUserId;
  @Column(nullable = false)
  private String title;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DocumentType documentType;
  private String documentNumber;
  private LocalDate issueDate;
  private LocalDate expiryDate;
  @Column(columnDefinition = "TEXT")
  private String notes;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Visibility visibility;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status;
  
  @PrePersist
  public void prePersist() {
    if (visibility == null) {
      visibility = Visibility.OWNER_ONLY;
    }
    if (status == null) {
      status = Status.ACTIVE;
    }
  }
}
