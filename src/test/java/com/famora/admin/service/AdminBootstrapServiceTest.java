package com.famora.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.famora.admin.config.AdminBootstrapProperties;
import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogService;
import com.famora.common.exception.AppException;
import com.famora.security.CurrentUserProvider;
import com.famora.user.entity.User;
import com.famora.user.entity.UserRole;
import com.famora.user.entity.UserStatus;
import com.famora.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapServiceTest {

  private static final String BOOTSTRAP_TOKEN = "bootstrap-secret-with-at-least-32-characters";

  @Mock private CurrentUserProvider currentUserProvider;
  @Mock private UserRepository userRepository;
  @Mock private AuditLogService auditLogService;

  private AdminBootstrapService service;

  @BeforeEach
  void setUp() {
    service = new AdminBootstrapService(currentUserProvider, userRepository, auditLogService,
        new AdminBootstrapProperties(BOOTSTRAP_TOKEN));
  }

  @Test
  void promotesAuthenticatedActiveUserWhenNoAdministratorExists() {
    User user = activeUser();
    when(currentUserProvider.getCurrentUserId()).thenReturn(user.getId());
    when(userRepository.findAllByIdForUpdate(List.of(user.getId()))).thenReturn(List.of(user));

    var response = service.bootstrapCurrentUser(BOOTSTRAP_TOKEN);

    assertThat(response.userId()).isEqualTo(user.getId());
    assertThat(response.email()).isEqualTo(user.getEmail());
    assertThat(response.role()).isEqualTo("ADMIN");
    assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
    verify(userRepository).acquireAdminBootstrapLock();
    verify(userRepository).existsByRole(UserRole.ADMIN);
    verify(userRepository).save(user);
    verify(auditLogService).log(null, user, AuditAction.USER_ADMIN_BOOTSTRAPPED, "users",
        user.getId(), "{\"role\":\"ADMIN\"}");
  }

  @Test
  void rejectsInvalidSecretBeforeAccessingBootstrapState() {
    assertThatThrownBy(() -> service.bootstrapCurrentUser("wrong-secret"))
        .isInstanceOfSatisfying(AppException.class, exception -> {
          assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
          assertThat(exception).hasMessage("Invalid admin bootstrap credentials");
        });

    verify(userRepository, never()).acquireAdminBootstrapLock();
    verify(userRepository, never()).existsByRole(any());
  }

  @Test
  void rejectsBootstrapWhenSecretIsNotConfigured() {
    service = new AdminBootstrapService(currentUserProvider, userRepository, auditLogService,
        new AdminBootstrapProperties(""));

    assertThatThrownBy(() -> service.bootstrapCurrentUser("anything"))
        .isInstanceOfSatisfying(AppException.class, exception -> {
          assertThat(exception.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
          assertThat(exception).hasMessage("Admin bootstrap is disabled");
        });

    verify(userRepository, never()).acquireAdminBootstrapLock();
  }

  @Test
  void rejectsWeakConfiguredBootstrapSecret() {
    assertThatThrownBy(() -> new AdminBootstrapProperties("too-short"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Admin bootstrap token must contain at least 32 characters");
  }

  @Test
  void rejectsBootstrapAfterAnAdministratorAlreadyExists() {
    when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(true);

    assertThatThrownBy(() -> service.bootstrapCurrentUser(BOOTSTRAP_TOKEN))
        .isInstanceOfSatisfying(AppException.class, exception -> {
          assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
          assertThat(exception).hasMessage("An administrator already exists");
        });

    verify(userRepository).acquireAdminBootstrapLock();
    verify(currentUserProvider, never()).getCurrentUserId();
    verify(userRepository, never()).save(any());
  }

  @Test
  void rejectsUserThatBecameInactiveAfterJwtAuthentication() {
    UUID userId = UUID.randomUUID();
    User lockedUser = activeUser();
    lockedUser.setStatus(UserStatus.LOCKED);
    when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
    when(userRepository.findAllByIdForUpdate(eq(List.of(userId))))
        .thenReturn(List.of(lockedUser));

    assertThatThrownBy(() -> service.bootstrapCurrentUser(BOOTSTRAP_TOKEN))
        .isInstanceOfSatisfying(AppException.class,
            exception -> assertThat(exception.status()).isEqualTo(HttpStatus.UNAUTHORIZED));

    verify(userRepository, never()).save(any());
  }

  private User activeUser() {
    User user = User.builder()
        .fullName("Admin User")
        .email("admin@example.com")
        .passwordHash("hash")
        .status(UserStatus.ACTIVE)
        .build();
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    return user;
  }
}
