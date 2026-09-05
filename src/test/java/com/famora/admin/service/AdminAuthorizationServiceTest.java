package com.famora.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.famora.security.CurrentUserProvider;
import com.famora.user.entity.User;
import com.famora.user.entity.UserRole;
import com.famora.user.entity.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;

@ExtendWith(MockitoExtension.class)
class AdminAuthorizationServiceTest {

  @Mock private CurrentUserProvider currentUserProvider;

  @Test
  void returnsCurrentUserWhenUserIsAdministrator() {
    User admin = user(UserRole.ADMIN);
    when(currentUserProvider.getCurrentUser()).thenReturn(admin);

    User result = new AdminAuthorizationService(currentUserProvider).requireAdmin();

    assertThat(result).isSameAs(admin);
  }

  @Test
  void rejectsRegularUser() {
    when(currentUserProvider.getCurrentUser()).thenReturn(user(UserRole.USER));

    assertThatThrownBy(
        () -> new AdminAuthorizationService(currentUserProvider).requireAdmin())
        .isInstanceOf(AuthorizationDeniedException.class)
        .hasMessage("Administrator access required");
  }

  private User user(UserRole role) {
    return User.builder()
        .fullName("User")
        .email("user@example.com")
        .passwordHash("hash")
        .status(UserStatus.ACTIVE)
        .role(role)
        .build();
  }
}
