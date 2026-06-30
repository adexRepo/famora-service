package com.famora.security;

import com.famora.common.exception.AppException;
import com.famora.family.dto.FamilyContext;
import com.famora.family.entity.Family;
import com.famora.family.entity.FamilyMember;
import com.famora.family.helper.FamilyMemberRole;
import com.famora.family.helper.FamilyMemberStatus;
import com.famora.family.repository.FamilyMemberRepository;
import com.famora.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FamilyContextService {
  
  public static final String FAMILY_ID_HEADER = "X-Family-Id";
  private final HttpServletRequest request;
  private final CurrentUserProvider currentUserProvider;
  private final FamilyMemberRepository familyMemberRepository;
  
  public Family getCurrentFamily() {
    UUID familyId = getCurrentFamilyId();
    UUID userId = currentUserProvider.getCurrentUserId();
    FamilyMember member = familyMemberRepository.findByFamilyIdAndUserIdAndStatus(familyId, userId,
            FamilyMemberStatus.ACTIVE)
        .orElseThrow(() -> new SecurityException("You are not member of this family"));
    return member.getFamily();
  }
  
  public UUID getCurrentFamilyId() {
    String rawFamilyId = request.getHeader(FAMILY_ID_HEADER);
    if (rawFamilyId == null || rawFamilyId.isBlank()) {
      throw new IllegalArgumentException("Missing X-Family-Id header");
    }
    try {
      return UUID.fromString(rawFamilyId);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Invalid X-Family-Id header");
    }
  }
  
  public boolean isCurrentUserFamilyMember(UUID familyId) {
    UUID userId = currentUserProvider.getCurrentUserId();
    return familyMemberRepository.existsByFamilyIdAndUserIdAndStatus(familyId, userId,
        FamilyMemberStatus.ACTIVE);
  }
  
  public void validateCurrentUserCanAccessFamily(UUID familyId) {
    if (!isCurrentUserFamilyMember(familyId)) {
      throw new SecurityException("Access denied to family");
    }
  }
  
  public FamilyContext require(String familyIdHeader) {
    if (!StringUtils.hasText(familyIdHeader)) {
      throw new AppException(HttpStatus.BAD_REQUEST, "X-Family-Id header is required");
    }
    UUID familyId;
    try {
      familyId = UUID.fromString(familyIdHeader);
      
      validateCurrentUserCanAccessFamily(familyId);
      
    } catch (Exception e) {
      throw new AppException(HttpStatus.BAD_REQUEST, "Invalid X-Family-Id");
    }
    User user = currentUserProvider.getCurrentUser();
    Family family = getCurrentFamily();
    FamilyMember m = familyMemberRepository.findByFamilyIdAndUserIdAndStatus(familyId, user.getId(),
            FamilyMemberStatus.ACTIVE)
        .orElseThrow(() -> new AppException(HttpStatus.FORBIDDEN,
            "User is not active member of selected family"));
    return new FamilyContext(family, user, m.getRole(), m.getRole() == FamilyMemberRole.OWNER);
  }
}
