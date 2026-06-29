package com.famora.family.repository;

import com.famora.family.dto.FamilyResponse;
import com.famora.family.entity.FamilyMember;
import com.famora.family.helper.FamilyMemberStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, UUID>,
    JpaSpecificationExecutor<FamilyMember> {
  
  @Query("""
      select new com.famora.family.dto.FamilyResponse(
          f.id,
          f.name,
          fm.role,
          count(fmAll.id)
      )
      from FamilyMember fm
      join fm.family f
      join FamilyMember fmAll
        on fmAll.family.id = f.id
       and fmAll.status = com.famora.family.helper.FamilyMemberStatus.ACTIVE
      where fm.user.id = :userId
        and fm.status = com.famora.family.helper.FamilyMemberStatus.ACTIVE
        and f.status = com.famora.common.helper.Status.ACTIVE
      group by
          f.id,
          f.name,
          fm.role,
          fm.createdAt
      order by fm.createdAt asc
      """)
  List<FamilyResponse> findActiveFamiliesByUserId(
      @Param("userId") UUID userId
  );
  
  Optional<FamilyMember> findByFamilyIdAndUserIdAndStatus(UUID familyId, UUID userId,
      FamilyMemberStatus status);
  
  boolean existsByFamilyIdAndUserIdAndStatus(UUID familyId, UUID userId, FamilyMemberStatus status);
}
