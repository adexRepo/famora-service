package com.famora.admin.service;

import com.famora.security.CurrentUserProvider;
import com.famora.user.entity.User;
import com.famora.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuthorizationService {

  private final CurrentUserProvider currentUserProvider;

  public User requireAdmin() {
    User user = currentUserProvider.getCurrentUser();
    if (user.getRole() != UserRole.ADMIN) {
      throw new AuthorizationDeniedException("Administrator access required");
    }
    return user;
  }
}
