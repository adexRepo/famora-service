package com.famora.family.repository;

import com.famora.family.entity.FamilyMember;
import com.famora.family.helper.FamilyMemberStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, UUID>,
    JpaSpecificationExecutor<FamilyMember> {
  
  @Query("""
          select fm
          from FamilyMember fm
          join fetch fm.family
          where fm.user.id = :userId
            and fm.status = com.famora.family.helper.FamilyMemberStatus.ACTIVE
      """)
  List<FamilyMember> findActiveFamiliesByUserId(@Param("userId") UUID userId);
  
  Optional<FamilyMember> findByFamilyIdAndUserIdAndStatus(UUID familyId, UUID userId,
      FamilyMemberStatus status);
  
  Optional<FamilyMember> findByUserIdAndDefaultFamilyTrueAndStatus(UUID userId,
      FamilyMemberStatus status);
  
  boolean existsByFamilyIdAndUserIdAndStatus(UUID familyId, UUID userId, FamilyMemberStatus status);
  
  boolean existsByUserIdAndDefaultFamilyTrueAndStatus(UUID userId, FamilyMemberStatus status);
  
  @Modifying
  @Query("""
      update FamilyMember fm
      set fm.defaultFamily = false
      where fm.user.id = :userId
        and fm.defaultFamily = true
      """)
  void clearDefaultByUserId(@Param("userId") UUID userId);
}
