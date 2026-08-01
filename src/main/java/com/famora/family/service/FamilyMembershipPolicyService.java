package com.famora.family.service;

import static com.famora.family.constant.FamilyErrorCodes.FAMILY_LIMIT_REACHED;

import com.famora.family.config.FamilyProperties;
import com.famora.family.dto.FamilyMembershipSummaryResponse;
import com.famora.family.exception.FamilyException;
import com.famora.family.helper.FamilyMemberStatus;
import com.famora.family.repository.FamilyMemberRepository;
import com.famora.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FamilyMembershipPolicyService {

  private final FamilyProperties familyProperties;
  private final FamilyMemberRepository familyMemberRepository;

  public FamilyMembershipSummaryResponse summary(User user) {
    long activeFamilyCount = activeFamilyCount(user);
    Integer maxFamilyCount = maxFamilyCount(user);
    return new FamilyMembershipSummaryResponse(
        activeFamilyCount,
        maxFamilyCount,
        maxFamilyCount == null || activeFamilyCount < maxFamilyCount,
        user.isFamilyLimitOverrideEnabled()
    );
  }

  public void requireCanCreateOrJoin(User user) {
    Integer maxFamilyCount = maxFamilyCount(user);
    if (maxFamilyCount != null && activeFamilyCount(user) >= maxFamilyCount) {
      throw FamilyException.conflict(
          FAMILY_LIMIT_REACHED,
          "You can only belong to " + maxFamilyCount + " active families.");
    }
  }

  public long activeFamilyCount(User user) {
    return familyMemberRepository.countByUserIdAndStatus(user.getId(), FamilyMemberStatus.ACTIVE);
  }

  public Integer maxFamilyCount(User user) {
    if (!user.isFamilyLimitOverrideEnabled()) {
      return familyProperties.effectiveMaxActiveFamilyPerUser();
    }
    return user.getMaxFamilyOverride();
  }
}
