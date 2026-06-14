package com.famora.family.dto;

import com.famora.family.entity.Family;
import com.famora.family.entity.FamilyMemberRole;
import com.famora.user.entity.User;

public record FamilyContext(Family familyId, User userId, FamilyMemberRole role, boolean owner) {

}
