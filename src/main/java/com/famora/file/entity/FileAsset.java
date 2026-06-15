package com.famora.file.entity;

import com.famora.common.entity.BaseEntity;
import com.famora.common.exception.Visibility;
import com.famora.common.helper.Status;
import com.famora.file.helper.FileType;
import com.famora.file.helper.StorageType;
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
@Table(name = "files")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileAsset extends BaseEntity {
  
  @Id
  @GeneratedValue
  @Column(name = "id", columnDefinition = "uuid")
  private UUID id;
  @Column(nullable = false)
  private UUID familyId;
  @Column(nullable = false)
  private UUID uploadedByUserId;
  @Column(nullable = false)
  private String originalName;
  @Column(nullable = false)
  private String storedName;
  @Column(nullable = false, columnDefinition = "TEXT")
  private String storagePath;
  @Column(nullable = false)
  private String mimeType;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FileType fileType;
  @Column(nullable = false)
  private long fileSize;
  private String fileHash;
  private String category;
  @Column(columnDefinition = "TEXT")
  private String notes;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Visibility visibility;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status;
  
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StorageType storageType;
  
  @Column(length = 100)
  private String bucketName;
  
  @Column(columnDefinition = "TEXT")
  private String objectKey;
  
  @PrePersist
  public void prePersist() {
    if (visibility == null) {
      visibility = Visibility.PRIVATE;
    }
    if (status == null) {
      status = Status.ACTIVE;
    }
  }
}
