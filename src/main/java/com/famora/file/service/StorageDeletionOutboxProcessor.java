package com.famora.file.service;

import com.famora.backup.service.BackupTempStorageCleanupService;
import com.famora.file.helper.StorageType;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class StorageDeletionOutboxProcessor {

  private static final int BATCH_SIZE = 20;

  private final JdbcTemplate jdbcTemplate;
  private final TransactionTemplate transactionTemplate;
  private final StorageService storageService;
  private final BackupTempStorageCleanupService backupTempStorageCleanupService;

  @Scheduled(fixedDelayString = "${app.storage.deletion-outbox-delay-ms:30000}")
  public void processDueTasks() {
    List<DeletionTask> tasks = transactionTemplate.execute(status -> claimDueTasks());
    if (tasks == null) {
      return;
    }
    tasks.forEach(this::process);
  }

  private List<DeletionTask> claimDueTasks() {
    return jdbcTemplate.query("""
        with candidates as (
          select id
          from famora.storage_deletion_outbox
          where processed_at is null
            and next_attempt_at <= now()
            and (locked_at is null or locked_at < now() - interval '15 minutes')
          order by created_at
          limit ?
          for update skip locked
        )
        update famora.storage_deletion_outbox task
        set locked_at = now()
        from candidates
        where task.id = candidates.id
        returning task.id, task.storage_type, task.bucket_name,
                  task.object_key, task.storage_path
        """, (rs, rowNum) -> new DeletionTask(
        rs.getObject("id", UUID.class),
        rs.getString("storage_type"),
        rs.getString("bucket_name"),
        rs.getString("object_key"),
        rs.getString("storage_path")), BATCH_SIZE);
  }

  private void process(DeletionTask task) {
    try {
      if ("BACKUP_TEMP".equals(task.storageType())) {
        backupTempStorageCleanupService.delete(task.storagePath());
      } else {
        StorageType storageType = StorageType.valueOf(task.storageType());
        String location = storageType == StorageType.MINIO
            ? task.objectKey() : task.storagePath();
        storageService.delete(storageType, task.bucketName(), location);
      }
      jdbcTemplate.update("""
          update famora.storage_deletion_outbox
          set processed_at = now(), locked_at = null, last_error = null
          where id = ?
          """, task.id());
    } catch (RuntimeException ex) {
      jdbcTemplate.update("""
          update famora.storage_deletion_outbox
          set attempts = attempts + 1,
              next_attempt_at = now() + interval '5 minutes' * least(attempts + 1, 12),
              locked_at = null,
              last_error = ?
          where id = ?
          """, ex.getClass().getSimpleName(), task.id());
    }
  }

  private record DeletionTask(UUID id, String storageType, String bucketName,
                              String objectKey, String storagePath) {
  }
}
