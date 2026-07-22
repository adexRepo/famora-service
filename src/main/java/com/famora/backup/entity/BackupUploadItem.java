package com.famora.backup.entity;

import com.famora.backup.enums.BackupUploadItemStatus;
import com.famora.common.entity.VisibleFamilyScopedEntity;
import com.famora.file.entity.FileAsset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "backup_upload_items")
public class BackupUploadItem extends VisibleFamilyScopedEntity {
  
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "session_id", nullable = false)
  private BackupUploadSession session;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "file_asset_id")
  private FileAsset fileAsset;
  
  @Column(name = "client_file_id", length = 120)
  private String clientFileId;
  
  @Column(name = "original_name", nullable = false)
  private String originalName;
  
  @Column(name = "original_mime_type", length = 120)
  private String originalMimeType;
  
  @Column(name = "file_size", nullable = false)
  private long fileSize;
  
  @Column(name = "expected_sha256", length = 64)
  private String expectedSha256;
  
  @Column(name = "chunk_size", nullable = false)
  private long chunkSize;
  
  @Column(name = "total_chunks", nullable = false)
  private int totalChunks;
  
  @Column(name = "received_chunks", nullable = false)
  private int receivedChunks;
  
  @Column(name = "uploaded_bytes", nullable = false)
  private long uploadedBytes;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "item_status", nullable = false, length = 30)
  private BackupUploadItemStatus itemStatus;
  
  @Column(length = 100)
  private String category;
  
  @Column(columnDefinition = "text")
  private String notes;
  
  @Column(name = "assembled_sha256", length = 64)
  private String assembledSha256;
  
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata_json", columnDefinition = "jsonb")
  private Map<String, Object> metadataJson;
  
  @Column(name = "completed_at")
  private OffsetDateTime completedAt;
}
