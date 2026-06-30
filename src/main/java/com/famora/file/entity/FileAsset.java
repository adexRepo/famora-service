package com.famora.file.entity;

import com.famora.common.entity.VisibleFamilyScopedEntity;
import com.famora.file.helper.FileType;
import com.famora.file.helper.StorageType;
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
@Table(name = "files")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FileAsset extends VisibleFamilyScopedEntity {
  
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
  @Column(columnDefinition = "TEXT")
  private String fileHash;
  private String category;
  @Column(columnDefinition = "TEXT")
  private String notes;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StorageType storageType;
  @Column(length = 100)
  private String bucketName;
  @Column(columnDefinition = "TEXT")
  private String objectKey;
  
}
