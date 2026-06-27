package com.famora.document.repository;

import com.famora.common.helper.Status;
import com.famora.document.entity.Document;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, UUID>,
    JpaSpecificationExecutor<Document> {
  
  Optional<Document> findByIdAndFamilyIdAndStatus(UUID id, UUID familyId, Status status);
  
  @Query("""
      select count(d)
      from Document d
      where d.family.id = :familyId
        and d.status = :status
        and (
          d.visibility = com.famora.common.helper.Visibility.FAMILY
          or (
            d.visibility = com.famora.common.helper.Visibility.PRIVATE
            and d.createdBy.id = :userId
          )
          or (
            :isOwner = true
            and d.visibility = com.famora.common.helper.Visibility.OWNER_ONLY
          )
        )""")
  long countVisibleDocuments(
      @Param("familyId") UUID familyId,
      @Param("userId") UUID userId,
      @Param("isOwner") boolean isOwner,
      @Param("status") Status status
  );
  
  @Query("""
      select count(d)
      from Document d
      where d.family.id = :familyId
        and d.status = :status
        and d.expiryDate between :startDate and :endDate
        and (
          d.visibility = com.famora.common.helper.Visibility.FAMILY
          or (
            d.visibility = com.famora.common.helper.Visibility.PRIVATE
            and d.createdBy.id = :userId
          )
          or (
            :isOwner = true
            and d.visibility = com.famora.common.helper.Visibility.OWNER_ONLY
          )
        )""")
  long countVisibleExpiringDocuments(
      @Param("familyId") UUID familyId,
      @Param("userId") UUID userId,
      @Param("isOwner") boolean isOwner,
      @Param("status") Status status,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate
  );
}
