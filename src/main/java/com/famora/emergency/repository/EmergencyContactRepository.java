package com.famora.emergency.repository;

import com.famora.common.helper.Status;
import com.famora.emergency.entity.EmergencyContact;
import com.famora.emergency.helper.EmergencyCategory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, UUID> {
  
  Optional<EmergencyContact> findByIdAndFamilyIdAndStatus(UUID id, UUID familyId, Status status);
  
  Page<EmergencyContact> findAllByFamilyIdAndStatus(UUID familyId, Status status,
      Pageable pageable);
  
  Page<EmergencyContact> findAllByFamilyIdAndStatusAndCategory(UUID familyId, Status status,
      EmergencyCategory category, Pageable pageable);
  
  Page<EmergencyContact> findAllByFamilyIdAndStatusAndNameContainingIgnoreCase(UUID familyId,
      Status status, String keyword, Pageable pageable);
  
  long countByFamilyIdAndStatus(UUID familyId, Status status);
}
