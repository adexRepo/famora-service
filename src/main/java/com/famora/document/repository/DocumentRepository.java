package com.famora.document.repository;

import com.famora.common.helper.Status;
import com.famora.document.entity.Document;
import com.famora.document.helper.DocumentType;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
  
  Optional<Document> findByIdAndFamilyIdAndStatus(UUID id, UUID familyId, Status status);
  
  Page<Document> findAllByFamilyIdAndStatus(UUID familyId, Status status, Pageable pageable);
  
  Page<Document> findAllByFamilyIdAndStatusAndDocumentType(UUID familyId, Status status,
      DocumentType type, Pageable pageable);
  
  Page<Document> findAllByFamilyIdAndStatusAndExpiryDateBetween(UUID familyId, Status status,
      LocalDate from, LocalDate to, Pageable pageable);
  
  long countByFamilyIdAndStatus(UUID familyId, Status status);
  
  long countByFamilyIdAndStatusAndExpiryDateBetween(UUID familyId, Status status, LocalDate from,
      LocalDate to);
  
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
