package com.famora.family.dto;

import com.famora.family.entity.Family;
import com.famora.family.helper.FamilyMemberRole;
import com.famora.user.entity.User;

public record FamilyContext(Family family, User user, FamilyMemberRole role, boolean owner) {

}
