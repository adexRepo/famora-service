package com.famora.document.entity;

import com.famora.common.entity.VisibleFamilyScopedEntity;
import com.famora.document.helper.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "documents")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Document extends VisibleFamilyScopedEntity {
  
  @Id
  @GeneratedValue
  @Column(name = "id", columnDefinition = "uuid")
  private UUID id;
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
  
}
