package com.famora.family.repository;

import com.famora.family.entity.FamilyMember;
import com.famora.family.helper.FamilyMemberRole;
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
          order by fm.defaultFamily desc, fm.joinedAt desc, fm.createdAt desc
      """)
  List<FamilyMember> findActiveFamiliesByUserId(@Param("userId") UUID userId);

  long countByUserIdAndStatus(UUID userId, FamilyMemberStatus status);
  
  Optional<FamilyMember> findByFamilyIdAndUserIdAndStatus(UUID familyId, UUID userId,
      FamilyMemberStatus status);

  Optional<FamilyMember> findByFamilyIdAndUserId(UUID familyId, UUID userId);

  Optional<FamilyMember> findByFamilyIdAndRoleAndStatus(UUID familyId, FamilyMemberRole role,
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

  @Modifying
  @Query("""
      update FamilyMember fm
      set fm.defaultFamily = false
      where fm.user.id = :userId
        and fm.family.id <> :familyId
        and fm.defaultFamily = true
      """)
  void clearDefaultByUserIdExceptFamily(@Param("userId") UUID userId,
      @Param("familyId") UUID familyId);

  @Modifying
  @Query("""
      update FamilyMember fm
      set fm.status = com.famora.family.helper.FamilyMemberStatus.LEFT,
          fm.defaultFamily = false,
          fm.removedAt = :deletedAt
      where fm.user.id = :userId
        and fm.status in (
          com.famora.family.helper.FamilyMemberStatus.ACTIVE,
          com.famora.family.helper.FamilyMemberStatus.PENDING,
          com.famora.family.helper.FamilyMemberStatus.LEAVE_REQUESTED,
          com.famora.family.helper.FamilyMemberStatus.TRANSFER_PENDING)
      """)
  int deactivateMembershipsForDeletedUser(@Param("userId") UUID userId,
      @Param("deletedAt") java.time.OffsetDateTime deletedAt);
}
