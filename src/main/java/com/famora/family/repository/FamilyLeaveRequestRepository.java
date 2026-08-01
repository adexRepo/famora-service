package com.famora.family.repository;

import com.famora.family.entity.FamilyLeaveRequest;
import com.famora.family.helper.FamilyLeaveRequestStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyLeaveRequestRepository extends JpaRepository<FamilyLeaveRequest, UUID> {

  boolean existsByFamilyIdAndRequesterIdAndRequestStatus(UUID familyId, UUID requesterId,
      FamilyLeaveRequestStatus requestStatus);

  Optional<FamilyLeaveRequest> findByIdAndFamilyId(UUID id, UUID familyId);

  @EntityGraph(attributePaths = {"family", "requester", "reviewedBy", "cancelledBy"})
  Page<FamilyLeaveRequest> findByFamilyId(UUID familyId, Pageable pageable);
}
