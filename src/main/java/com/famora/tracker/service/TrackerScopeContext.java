package com.famora.tracker.service;

import com.famora.business.entity.Business;
import com.famora.business.entity.BusinessMember;
import com.famora.family.entity.Family;
import com.famora.family.entity.FamilyMember;
import com.famora.tracker.enums.TrackerScopeType;
import com.famora.user.entity.User;

public record TrackerScopeContext(
    TrackerScopeType scopeType,
    Family family,
    Business business,
    FamilyMember familyMember,
    BusinessMember businessMember,
    User user
) {
}
