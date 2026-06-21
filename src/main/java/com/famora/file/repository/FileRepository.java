package com.famora.file.repository;

import com.famora.common.helper.Status;
import com.famora.common.helper.Visibility;
import com.famora.file.entity.FileAsset;
import com.famora.file.helper.FileType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FileRepository extends JpaRepository<FileAsset, UUID> {
  
  Optional<FileAsset> findByIdAndFamilyIdAndStatus(UUID id, UUID familyId, Status status);
  
  Page<FileAsset> findAllByFamilyIdAndStatus(UUID familyId, Status status, Pageable pageable);
  
  Page<FileAsset> findAllByFamilyIdAndStatusAndFileType(UUID familyId, Status status,
      FileType fileType, Pageable pageable);
  
  Page<FileAsset> findAllByFamilyIdAndStatusAndVisibility(UUID familyId, Status status,
      Visibility visibility, Pageable pageable);
  
  Page<FileAsset> findAllByFamilyIdAndStatusAndFileTypeAndVisibility(UUID familyId, Status status,
      FileType fileType, Visibility visibility, Pageable pageable);
  
  @Query("""
    select count(f)
    from FileAsset f
    where f.family.id = :familyId
      and f.status = :status
      and (
        f.visibility = com.famora.common.helper.Visibility.FAMILY
        or (
          f.visibility = com.famora.common.helper.Visibility.PRIVATE
          and f.createdBy.id = :userId
        )
        or (
          :isOwner = true
          and f.visibility = com.famora.common.helper.Visibility.OWNER_ONLY
        )
      )""")
  long countVisibleFiles(
      @Param("familyId") UUID familyId,
      @Param("userId") UUID userId,
      @Param("isOwner") boolean isOwner,
      @Param("status") Status status
  );
  
  @Query("""
    select coalesce(sum(f.fileSize), 0)
    from FileAsset f
    where f.family.id = :familyId
      and f.status = :status
      and (
        f.visibility = com.famora.common.helper.Visibility.FAMILY
        or (
          f.visibility = com.famora.common.helper.Visibility.PRIVATE
          and f.createdBy.id = :userId
        )
        or (
          :isOwner = true
          and f.visibility = com.famora.common.helper.Visibility.OWNER_ONLY
        )
      )""")
  long sumVisibleFileSize(
      @Param("familyId") UUID familyId,
      @Param("userId") UUID userId,
      @Param("isOwner") boolean isOwner,
      @Param("status") Status status
  );
}
