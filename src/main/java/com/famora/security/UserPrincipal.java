package com.famora.security;

import com.famora.user.entity.User;
import com.famora.user.entity.UserRole;
import com.famora.user.entity.UserStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@RequiredArgsConstructor
public class UserPrincipal implements UserDetails {
  
  private final UUID id;
  private final String email;
  private final String password;
  private final UserStatus status;
  private final UserRole role;
  
  public static UserPrincipal from(User user) {
    return new UserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(),
        user.getStatus(), user.getRole() == null ? UserRole.USER : user.getRole());
  }
  
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }
  
  @Override
  public String getUsername() {
    return email;
  }
  
  @Override
  public boolean isAccountNonExpired() {
    return status != UserStatus.DELETED;
  }
  
  @Override
  public boolean isAccountNonLocked() {
    return status != UserStatus.LOCKED;
  }
  
  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }
  
  @Override
  public boolean isEnabled() {
    return status == UserStatus.ACTIVE;
  }
}
