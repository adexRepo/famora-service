package com.famora.admin.service;

import com.famora.admin.config.AdminBootstrapProperties;
import com.famora.admin.dto.AdminBootstrapResponse;
import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogService;
import com.famora.common.exception.AppException;
import com.famora.security.CurrentUserProvider;
import com.famora.user.entity.User;
import com.famora.user.entity.UserRole;
import com.famora.user.entity.UserStatus;
import com.famora.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminBootstrapService {

  private final CurrentUserProvider currentUserProvider;
  private final UserRepository userRepository;
  private final AuditLogService auditLogService;
  private final AdminBootstrapProperties properties;

  @Transactional
  public AdminBootstrapResponse bootstrapCurrentUser(String suppliedToken) {
    validateToken(suppliedToken);
    userRepository.acquireAdminBootstrapLock();
    if (userRepository.existsByRole(UserRole.ADMIN)) {
      throw new AppException(HttpStatus.CONFLICT, "An administrator already exists");
    }

    UUID currentUserId = currentUserProvider.getCurrentUserId();
    User user = userRepository.findAllByIdForUpdate(List.of(currentUserId)).stream()
        .filter(candidate -> candidate.getStatus() == UserStatus.ACTIVE)
        .findFirst()
        .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

    user.setRole(UserRole.ADMIN);
    userRepository.save(user);
    auditLogService.log(null, user, AuditAction.USER_ADMIN_BOOTSTRAPPED, "users", user.getId(),
        "{\"role\":\"ADMIN\"}");
    return new AdminBootstrapResponse(user.getId(), user.getEmail(), user.getRole().name());
  }

  private void validateToken(String suppliedToken) {
    String configuredToken = properties.token();
    if (configuredToken.isBlank()) {
      throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "Admin bootstrap is disabled");
    }
    byte[] configuredBytes = configuredToken.getBytes(StandardCharsets.UTF_8);
    byte[] suppliedBytes = suppliedToken == null
        ? new byte[0]
        : suppliedToken.getBytes(StandardCharsets.UTF_8);
    if (!MessageDigest.isEqual(configuredBytes, suppliedBytes)) {
      throw new AppException(HttpStatus.FORBIDDEN, "Invalid admin bootstrap credentials");
    }
  }
}
