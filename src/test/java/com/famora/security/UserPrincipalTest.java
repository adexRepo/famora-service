package com.famora.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.famora.user.entity.User;
import com.famora.user.entity.UserRole;
import com.famora.user.entity.UserStatus;
import org.junit.jupiter.api.Test;

class UserPrincipalTest {

  @Test
  void exposesAdministratorAuthorityFromCurrentDatabaseRole() {
    User user = User.builder()
        .fullName("Admin")
        .email("admin@example.com")
        .passwordHash("hash")
        .status(UserStatus.ACTIVE)
        .role(UserRole.ADMIN)
        .build();

    assertThat(UserPrincipal.from(user).getAuthorities())
        .extracting(Object::toString)
        .containsExactly("ROLE_ADMIN");
  }

  @Test
  void defaultsMissingLegacyRoleToRegularUserAuthority() {
    User user = User.builder()
        .fullName("User")
        .email("user@example.com")
        .passwordHash("hash")
        .status(UserStatus.ACTIVE)
        .role(null)
        .build();

    assertThat(UserPrincipal.from(user).getAuthorities())
        .extracting(Object::toString)
        .containsExactly("ROLE_USER");
  }
}
