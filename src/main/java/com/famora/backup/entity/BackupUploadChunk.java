package com.famora.backup.entity;

import com.famora.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "backup_upload_chunks")
public class BackupUploadChunk extends AuditableEntity {
  
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "session_id", nullable = false)
  private BackupUploadSession session;
  
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "item_id", nullable = false)
  private BackupUploadItem item;
  
  @Column(name = "chunk_number", nullable = false)
  private int chunkNumber;
  
  @Column(name = "chunk_size", nullable = false)
  private long chunkSize;
  
  @Column(length = 64)
  private String sha256;
  
  @Column(name = "storage_path", nullable = false, columnDefinition = "text")
  private String storagePath;
}
