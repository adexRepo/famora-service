package com.famora.security;

import com.famora.common.exception.ResourceNotFoundException;
import com.famora.user.entity.User;
import com.famora.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
  
  private final UserRepository userRepository;
  
  public UUID getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
      throw new AuthorizationDeniedException("Unauthenticated user");
    }
    return principal.getId();
  }
  
  public User getCurrentUser() {
    return userRepository.findByIdAndDeletedAtIsNull(getCurrentUserId())
        .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
  }
}
