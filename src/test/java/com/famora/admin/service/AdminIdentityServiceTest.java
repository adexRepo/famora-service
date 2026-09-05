package com.famora.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.famora.user.entity.User;
import com.famora.user.entity.UserRole;
import com.famora.user.entity.UserStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminIdentityServiceTest {

  @Mock private AdminAuthorizationService authorizationService;

  @Test
  void returnsCurrentAdministratorIdentity() {
    User admin = User.builder()
        .fullName("Admin")
        .email("admin@example.com")
        .passwordHash("hash")
        .status(UserStatus.ACTIVE)
        .role(UserRole.ADMIN)
        .build();
    ReflectionTestUtils.setField(admin, "id", UUID.randomUUID());
    when(authorizationService.requireAdmin()).thenReturn(admin);

    var response = new AdminIdentityService(authorizationService).getMe();

    assertThat(response.userId()).isEqualTo(admin.getId());
    assertThat(response.email()).isEqualTo(admin.getEmail());
    assertThat(response.role()).isEqualTo("ADMIN");
  }
}
