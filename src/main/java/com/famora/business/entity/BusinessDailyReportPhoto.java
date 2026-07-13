package com.famora.business.entity;

import com.famora.common.entity.BusinessScopedEntity;
import com.famora.file.helper.FileType;
import com.famora.file.helper.StorageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "business_daily_report_photos")
public class BusinessDailyReportPhoto extends BusinessScopedEntity {
  
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "daily_report_id", nullable = false)
  private BusinessDailyReport dailyReport;
  
  @Column(name = "original_name", nullable = false, length = 255)
  private String originalName;
  
  @Column(name = "original_extension", length = 20)
  private String originalExtension;
  
  @Column(name = "original_mime_type", length = 120)
  private String originalMimeType;
  
  @Column(name = "stored_name", nullable = false, length = 255)
  private String storedName;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "storage_type", nullable = false, length = 30)
  private StorageType storageType;
  
  @Column(name = "storage_path", columnDefinition = "text")
  private String storagePath;
  
  @Column(name = "bucket_name", length = 100)
  private String bucketName;
  
  @Column(name = "object_key", columnDefinition = "text")
  private String objectKey;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "file_type", nullable = false, length = 30)
  private FileType fileType;
  
  @Column(name = "mime_type", nullable = false, length = 120)
  private String mimeType;
  
  @Column(name = "file_size", nullable = false)
  private long fileSize;
  
  @Column(name = "file_hash", columnDefinition = "text")
  private String fileHash;
  
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata_json", columnDefinition = "jsonb")
  private Map<String, Object> metadataJson;
}
