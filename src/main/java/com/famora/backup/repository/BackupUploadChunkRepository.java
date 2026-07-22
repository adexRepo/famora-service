package com.famora.backup.repository;

import com.famora.backup.entity.BackupUploadChunk;
import com.famora.common.helper.Status;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BackupUploadChunkRepository extends JpaRepository<BackupUploadChunk, UUID> {
  
  Optional<BackupUploadChunk> findByItemIdAndChunkNumberAndStatus(UUID itemId, int chunkNumber,
      Status status);
  
  List<BackupUploadChunk> findByItemIdAndStatusOrderByChunkNumberAsc(UUID itemId, Status status);
  
  long countByItemIdAndStatus(UUID itemId, Status status);
  
  @Query("""
      select coalesce(sum(c.chunkSize), 0)
      from BackupUploadChunk c
      where c.item.id = :itemId and c.status = :status
      """)
  long sumChunkSize(UUID itemId, Status status);
}
