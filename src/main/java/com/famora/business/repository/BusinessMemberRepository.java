package com.famora.business.repository;

import com.famora.business.entity.BusinessMember;
import com.famora.common.helper.Status;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BusinessMemberRepository extends JpaRepository<BusinessMember, UUID>,
    JpaSpecificationExecutor<BusinessMember> {
  
  Optional<BusinessMember> findByBusinessIdAndUserIdAndStatus(UUID businessId, UUID userId,
      Status status);
  
  Optional<BusinessMember> findByBusinessIdAndUserId(UUID businessId, UUID userId);
  
  List<BusinessMember> findByBusinessIdAndStatus(UUID businessId, Status status);
  
  boolean existsByBusinessIdAndUserIdAndStatus(UUID businessId, UUID userId, Status status);
}
