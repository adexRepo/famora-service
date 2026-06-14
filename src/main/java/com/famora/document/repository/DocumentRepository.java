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
}
