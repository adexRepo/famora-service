package com.famora.admin.service;

import com.famora.admin.dto.AdminMeResponse;
import com.famora.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminIdentityService {

  private final AdminAuthorizationService authorizationService;

  @Transactional(readOnly = true)
  public AdminMeResponse getMe() {
    User admin = authorizationService.requireAdmin();
    return new AdminMeResponse(admin.getId(), admin.getEmail(), admin.getRole().name());
  }
}
