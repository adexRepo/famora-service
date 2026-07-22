package com.famora.backup.repository;

import com.famora.backup.entity.BackupUploadSession;
import com.famora.common.helper.Status;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BackupUploadSessionRepository extends JpaRepository<BackupUploadSession, UUID> {
  
  Optional<BackupUploadSession> findByIdAndFamilyIdAndStatus(UUID id, UUID familyId,
      Status status);
  
  Page<BackupUploadSession> findByFamilyIdAndStatus(UUID familyId, Status status,
      Pageable pageable);
}
