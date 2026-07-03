package com.famora.business.repository;

import com.famora.business.entity.BusinessMember;
import com.famora.common.helper.Status;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessMemberRepository extends JpaRepository<BusinessMember, UUID>,
    JpaSpecificationExecutor<BusinessMember> {
  
  Optional<BusinessMember> findByBusinessIdAndUserIdAndStatus(UUID businessId, UUID userId,
      Status status);
  
  Optional<BusinessMember> findByUserIdAndDefaultBusinessTrueAndStatus(UUID userId,
      Status status);
  
  Optional<BusinessMember> findByBusinessIdAndUserId(UUID businessId, UUID userId);
  
  List<BusinessMember> findByBusinessIdAndStatus(UUID businessId, Status status);
  
  boolean existsByBusinessIdAndUserIdAndStatus(UUID businessId, UUID userId, Status status);
  
  boolean existsByUserIdAndDefaultBusinessTrueAndStatus(UUID userId, Status status);
  
  @Modifying
  @Query("""
      update BusinessMember m
      set m.defaultBusiness = false
      where m.userId = :userId
        and m.defaultBusiness = true
      """)
  void clearDefaultByUserId(@Param("userId") UUID userId);
}
