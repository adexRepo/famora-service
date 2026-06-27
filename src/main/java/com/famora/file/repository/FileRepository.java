package com.famora.file.repository;

import com.famora.common.helper.Status;
import com.famora.file.entity.FileAsset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FileRepository extends JpaRepository<FileAsset, UUID>,
    JpaSpecificationExecutor<FileAsset> {
  
  Optional<FileAsset> findByIdAndFamilyIdAndStatus(UUID id, UUID familyId, Status status);
  
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
