package com.famora.security;

import com.famora.common.exception.AppException;
import com.famora.common.exception.ResourceNotFoundException;
import com.famora.user.entity.User;
import com.famora.user.entity.UserStatus;
import com.famora.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserProvider {
  
  private final UserRepository userRepository;
  
  public static UserPrincipal currentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal p)) {
      throw new AppException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
    return p;
  }
  
  public UUID getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
      throw new AuthorizationDeniedException("Unauthenticated user");
    }
    return principal.getId();
  }
  
  public User getCurrentUser() {
    return userRepository.findByIdAndStatus(getCurrentUserId(), UserStatus.ACTIVE)
        .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
  }
}
