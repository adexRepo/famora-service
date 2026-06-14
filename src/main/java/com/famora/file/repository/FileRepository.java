package com.famora.file.repository;

import com.famora.common.exception.Visibility;
import com.famora.common.helper.Status;
import com.famora.file.entity.FileAsset;
import com.famora.file.helper.FileType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileAsset, UUID> {
  
  Optional<FileAsset> findByIdAndFamilyIdAndStatus(UUID id, UUID familyId, Status status);
  
  Page<FileAsset> findAllByFamilyIdAndStatus(UUID familyId, Status status, Pageable pageable);
  
  Page<FileAsset> findAllByFamilyIdAndStatusAndFileType(UUID familyId, Status status,
      FileType fileType, Pageable pageable);
  
  Page<FileAsset> findAllByFamilyIdAndStatusAndVisibility(UUID familyId, Status status,
      Visibility visibility, Pageable pageable);
  
  Page<FileAsset> findAllByFamilyIdAndStatusAndFileTypeAndVisibility(UUID familyId, Status status,
      FileType fileType, Visibility visibility, Pageable pageable);
  
  long countByFamilyIdAndStatus(UUID familyId, Status status);
  
  long countByFamilyIdAndStatusAndFileType(UUID familyId, Status status, FileType type);
  
  long countByFamilyIdAndStatusAndOriginalNameContainingIgnoreCase(UUID familyId, Status status,
      String keyword);
}
