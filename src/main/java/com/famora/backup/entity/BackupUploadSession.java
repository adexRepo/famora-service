package com.famora.backup.entity;

import com.famora.backup.enums.BackupUploadSessionStatus;
import com.famora.common.entity.VisibleFamilyScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
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
@Table(name = "backup_upload_sessions")
public class BackupUploadSession extends VisibleFamilyScopedEntity {
  
  @Enumerated(EnumType.STRING)
  @Column(name = "upload_status", nullable = false, length = 30)
  private BackupUploadSessionStatus uploadStatus;
  
  @Column(name = "total_files", nullable = false)
  private int totalFiles;
  
  @Column(name = "completed_files", nullable = false)
  private int completedFiles;
  
  @Column(name = "failed_files", nullable = false)
  private int failedFiles;
  
  @Column(name = "total_bytes", nullable = false)
  private long totalBytes;
  
  @Column(name = "uploaded_bytes", nullable = false)
  private long uploadedBytes;
  
  @Column(length = 100)
  private String category;
  
  @Column(columnDefinition = "text")
  private String notes;
  
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata_json", columnDefinition = "jsonb")
  private Map<String, Object> metadataJson;
  
  @Column(name = "completed_at")
  private OffsetDateTime completedAt;
  
  @Column(name = "cancelled_at")
  private OffsetDateTime cancelledAt;
}
