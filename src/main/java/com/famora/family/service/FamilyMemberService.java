package com.famora.family.service;

import com.famora.family.dto.FamilyContext;
import com.famora.family.dto.FamilyMemberResponse;
import com.famora.family.entity.FamilyMember;
import com.famora.family.helper.FamilyMemberRole;
import com.famora.family.helper.FamilyMemberStatus;
import com.famora.family.repository.FamilyMemberRepository;
import com.famora.family.spec.FamilyMemberSpecifications;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FamilyMemberService {
  
  private final FamilyMemberRepository familyMemberRepository;
  
  @Transactional(readOnly = true)
  public Page<FamilyMemberResponse> list(FamilyContext ctx, String keyword, FamilyMemberRole role,
      FamilyMemberStatus status, org.springframework.data.domain.Pageable pageable) {
    UUID familyId = ctx.family().getId();
    
    FamilyMemberStatus selectedStatus = status == null ? FamilyMemberStatus.ACTIVE : status;
    
    Specification<FamilyMember> spec = Specification.where(
            FamilyMemberSpecifications.family(familyId))
        .and(FamilyMemberSpecifications.status(selectedStatus))
        .and(FamilyMemberSpecifications.role(role))
        .and(FamilyMemberSpecifications.keyword(keyword));
    
    return familyMemberRepository.findAll(spec, pageable).map(this::toResponse);
  }
  
  private FamilyMemberResponse toResponse(FamilyMember member) {
    return new FamilyMemberResponse(member.getId(), member.getUser().getId(),
        member.getUser().getFullName(), member.getUser().getEmail(), member.getRole(),
        member.getStatus(), member.isDefaultFamily(), member.getJoinedAt(), member.getCreatedAt(),
        member.getUpdatedAt());
  }
}
