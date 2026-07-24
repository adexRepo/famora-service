package com.famora.emergency.repository;

import com.famora.common.helper.Status;
import com.famora.emergency.entity.EmergencyContact;
import com.famora.emergency.helper.EmergencyCategory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, UUID>,
    JpaSpecificationExecutor<EmergencyContact> {
  
  Optional<EmergencyContact> findByIdAndFamilyIdAndStatus(UUID id, UUID familyId, Status status);

  long countByFamilyIdAndStatus(UUID familyId, Status status);
}
