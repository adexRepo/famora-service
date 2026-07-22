package com.famora.backup.repository;

import com.famora.backup.entity.BackupUploadItem;
import com.famora.backup.enums.BackupUploadItemStatus;
import com.famora.common.helper.Status;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BackupUploadItemRepository extends JpaRepository<BackupUploadItem, UUID> {
  
  List<BackupUploadItem> findBySessionIdAndStatusOrderByCreatedAtAsc(UUID sessionId,
      Status status);
  
  Optional<BackupUploadItem> findByIdAndSessionIdAndStatus(UUID id, UUID sessionId,
      Status status);
  
  long countBySessionIdAndStatusAndItemStatus(UUID sessionId, Status status,
      BackupUploadItemStatus itemStatus);
  
  @Query("""
      select coalesce(sum(i.uploadedBytes), 0)
      from BackupUploadItem i
      where i.session.id = :sessionId and i.status = :status
      """)
  long sumUploadedBytes(UUID sessionId, Status status);
}
